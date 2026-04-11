(function () {
    const PLUGIN_ID = "animatruc_exporter";
    const PACK_VERSION = 2;
    const TPS = 20;

    let exportAction = null;
    let validateAction = null;
    let previewAction = null;
    let openBbmodelAction = null;
    let importGltfAction = null;

    function toNumber(value, fallback) {
        if (typeof value === "number" && Number.isFinite(value)) {
            return value;
        }

        if (typeof value === "string") {
            const parsed = Number.parseFloat(value);

            if (Number.isFinite(parsed)) {
                return parsed;
            }
        }

        return fallback;
    }

    function toVec3(values, fallbackX, fallbackY, fallbackZ) {
        if (!Array.isArray(values)) {
            return [fallbackX, fallbackY, fallbackZ];
        }

        return [
            toNumber(values[0], fallbackX),
            toNumber(values[1], fallbackY),
            toNumber(values[2], fallbackZ)
        ];
    }

    function axisAngleQuat(ax, ay, az, radians) {
        const half = radians * 0.5;
        const sin = Math.sin(half);
        const cos = Math.cos(half);

        return [ax * sin, ay * sin, az * sin, cos];
    }

    function multiplyQuat(a, b) {
        const ax = a[0], ay = a[1], az = a[2], aw = a[3];
        const bx = b[0], by = b[1], bz = b[2], bw = b[3];

        return [
            aw * bx + ax * bw + ay * bz - az * by,
            aw * by - ax * bz + ay * bw + az * bx,
            aw * bz + ax * by - ay * bx + az * bw,
            aw * bw - ax * bx - ay * by - az * bz
        ];
    }

    function normalizeQuat(quat) {
        const length = Math.sqrt(quat[0] * quat[0] + quat[1] * quat[1] + quat[2] * quat[2] + quat[3] * quat[3]);

        if (length <= 0) {
            return [0, 0, 0, 1];
        }

        return [quat[0] / length, quat[1] / length, quat[2] / length, quat[3] / length];
    }

    function quaternionFromEulerDegrees(x, y, z) {
        const degToRad = Math.PI / 180;
        const qx = axisAngleQuat(1, 0, 0, toNumber(x, 0) * degToRad);
        const qy = axisAngleQuat(0, 1, 0, toNumber(y, 0) * degToRad);
        const qz = axisAngleQuat(0, 0, 1, toNumber(z, 0) * degToRad);

        return normalizeQuat(multiplyQuat(multiplyQuat(qy, qx), qz));
    }

    function normalizeInterpolation(value) {
        const normalized = String(value || "LINEAR").trim().toUpperCase();
        return normalized === "STEP" ? "STEP" : "LINEAR";
    }

    function normalizeChannel(channel) {
        const normalized = String(channel || "").trim().toLowerCase();

        if (normalized === "rotation") {
            return "rotation";
        }
        if (normalized === "position" || normalized === "translation" || normalized === "location") {
            return "translation";
        }
        if (normalized === "scale") {
            return "scale";
        }

        return null;
    }

    function resolveLoop(loopValue) {
        if (typeof loopValue === "boolean") {
            return loopValue;
        }

        const normalized = String(loopValue || "loop").trim().toLowerCase();
        return !(normalized === "once" || normalized === "hold" || normalized === "hold_on_last_frame");
    }

    function sanitizeBaseFileName(value) {
        let name = String(value || "animatruc_project").trim();

        if (name.length === 0) {
            return "animatruc_project";
        }

        name = name
            .replace(/\.animatrucpack\.json$/i, "")
            .replace(/\.animatrucpack$/i, "")
            .replace(/\.animatruc\.json$/i, "")
            .replace(/\.animatruc$/i, "")
            .replace(/[<>:"/\\|?*\x00-\x1F]/g, "_")
            .replace(/\s+/g, "_");

        return name.length > 0 ? name : "animatruc_project";
    }

    function extractKeyframeVec3(keyframe, channel) {
        const fallback = channel === "scale" ? [1, 1, 1] : [0, 0, 0];
        const points = keyframe && Array.isArray(keyframe.data_points) ? keyframe.data_points : [];
        const point = points.length > 0 ? points[0] : keyframe;

        if (!point) {
            return fallback;
        }

        return [
            toNumber(point.x, fallback[0]),
            toNumber(point.y, fallback[1]),
            toNumber(point.z, fallback[2])
        ];
    }

    function buildSkeleton() {
        return Group.all.map(function (group) {
            const parent = group.parent instanceof Group ? group.parent.name : null;
            const pivot = toVec3(group.origin, 0, 0, 0);
            const rotationEuler = toVec3(group.rotation, 0, 0, 0);
            const scale = toVec3(group.scale, 1, 1, 1);

            return {
                name: group.name,
                parent: parent,
                pivot: pivot,
                bindPose: {
                    translation: [0, 0, 0],
                    rotation: quaternionFromEulerDegrees(rotationEuler[0], rotationEuler[1], rotationEuler[2]),
                    scale: scale
                }
            };
        });
    }

    function buildModelCubes() {
        if (!Cube || !Array.isArray(Cube.all)) {
            return [];
        }

        return Cube.all.map(function (cube, index) {
            const parent = cube.parent instanceof Group ? cube.parent.name : null;

            return {
                name: cube.name || ("cube_" + index),
                bone: parent || "root",
                from: toVec3(cube.from, 0, 0, 0),
                to: toVec3(cube.to, 0, 0, 0),
                inflate: toNumber(cube.inflate, 0),
                mirror: Boolean(cube.mirror_uv || cube.mirror)
            };
        });
    }

    function readMeshVertices(mesh) {
        const vertices = [];
        const indicesByKey = {};

        if (!mesh || !mesh.vertices || typeof mesh.vertices !== "object") {
            return { vertices: vertices, indicesByKey: indicesByKey };
        }

        Object.keys(mesh.vertices).forEach(function (key, index) {
            const raw = mesh.vertices[key];
            const vec = Array.isArray(raw)
                ? toVec3(raw, 0, 0, 0)
                : [toNumber(raw.x, 0), toNumber(raw.y, 0), toNumber(raw.z, 0)];

            indicesByKey[key] = index;
            vertices.push(vec);
        });

        return { vertices: vertices, indicesByKey: indicesByKey };
    }

    function buildMeshFaces(mesh, indicesByKey) {
        const faces = [];

        if (!mesh || !mesh.faces || typeof mesh.faces !== "object") {
            return faces;
        }

        Object.keys(mesh.faces).forEach(function (faceKey) {
            const face = mesh.faces[faceKey];

            if (!face || !Array.isArray(face.vertices) || face.vertices.length < 3) {
                return;
            }

            const indices = [];
            const uvs = [];

            face.vertices.forEach(function (vertexKey) {
                const index = indicesByKey[vertexKey];

                if (typeof index === "number") {
                    indices.push(index);

                    if (face.uv && face.uv[vertexKey]) {
                        const uv = face.uv[vertexKey];
                        uvs.push([toNumber(uv[0], 0), toNumber(uv[1], 0)]);
                    }
                }
            });

            if (indices.length >= 3) {
                faces.push({
                    indices: indices,
                    uvs: uvs
                });
            }
        });

        return faces;
    }

    function buildModelMeshes() {
        if (typeof Mesh === "undefined" || !Array.isArray(Mesh.all)) {
            return [];
        }

        return Mesh.all.map(function (mesh, index) {
            const parent = mesh.parent instanceof Group ? mesh.parent.name : "root";
            const origin = toVec3(mesh.origin, 0, 0, 0);
            const parsedVertices = readMeshVertices(mesh);
            const faces = buildMeshFaces(mesh, parsedVertices.indicesByKey);

            return {
                name: mesh.name || ("mesh_" + index),
                bone: parent,
                origin: origin,
                vertices: parsedVertices.vertices,
                faces: faces
            };
        }).filter(function (mesh) {
            return mesh.vertices.length > 0 && mesh.faces.length > 0;
        });
    }

    function buildTrackFromAnimator(animator) {
        const track = {
            translation: [],
            rotation: [],
            scale: []
        };
        const keyframes = animator && Array.isArray(animator.keyframes) ? animator.keyframes : [];

        keyframes.forEach(function (keyframe) {
            const channel = normalizeChannel(keyframe.channel);

            if (!channel) {
                return;
            }

            const tick = Math.max(0, toNumber(keyframe.time, 0) * TPS);
            const interpolation = normalizeInterpolation(keyframe.interpolation);
            const vec3 = extractKeyframeVec3(keyframe, channel);

            if (channel === "rotation") {
                track.rotation.push({
                    tick: tick,
                    interpolation: interpolation,
                    value: quaternionFromEulerDegrees(vec3[0], vec3[1], vec3[2])
                });
            }
            else if (channel === "translation") {
                track.translation.push({
                    tick: tick,
                    interpolation: interpolation,
                    value: vec3
                });
            }
            else if (channel === "scale") {
                track.scale.push({
                    tick: tick,
                    interpolation: interpolation,
                    value: vec3
                });
            }
        });

        track.translation.sort(function (a, b) { return a.tick - b.tick; });
        track.rotation.sort(function (a, b) { return a.tick - b.tick; });
        track.scale.sort(function (a, b) { return a.tick - b.tick; });

        return track;
    }

    function hasTrackContent(track) {
        return track.translation.length > 0 || track.rotation.length > 0 || track.scale.length > 0;
    }

    function buildClips() {
        const groupsByUuid = {};
        Group.all.forEach(function (group) {
            groupsByUuid[group.uuid] = group;
        });

        return Animation.all.map(function (animation, index) {
            const tracks = {};
            const animators = animation && animation.animators ? animation.animators : {};

            Object.keys(animators).forEach(function (animatorId) {
                const animator = animators[animatorId];

                if (!animator || animator.type !== "bone") {
                    return;
                }

                const group = groupsByUuid[animatorId];
                const boneName = group ? group.name : (animator.name || animatorId);
                const track = buildTrackFromAnimator(animator);

                if (hasTrackContent(track)) {
                    tracks[boneName] = track;
                }
            });

            return {
                name: animation.name || ("clip_" + index),
                lengthTicks: Math.max(0, toNumber(animation.length, 0) * TPS),
                looping: resolveLoop(animation.loop),
                tracks: tracks
            };
        });
    }

    function buildPack() {
        const projectName = Project && Project.name ? Project.name : "animatruc_project";

        return {
            format: "animatruc-pack",
            version: PACK_VERSION,
            meta: {
                source: "blockbench",
                pluginId: PLUGIN_ID,
                projectName: projectName,
                exportedAt: new Date().toISOString()
            },
            skeleton: {
                bones: buildSkeleton()
            },
            model: {
                cubes: buildModelCubes(),
                meshes: buildModelMeshes()
            },
            clips: buildClips()
        };
    }

    function projectValidationWarnings() {
        const warnings = [];
        const names = {};
        const groupsByUuid = {};

        Group.all.forEach(function (group) {
            if (names[group.name]) {
                warnings.push("Nom d'os dupliqué : " + group.name);
            }

            names[group.name] = true;
            groupsByUuid[group.uuid] = group;
        });

        if (Group.all.length === 0) {
            warnings.push("Aucun os trouvé. Crée au moins un groupe pour le squelette AnimaTruc.");
        }

        const cubeCount = Cube && Array.isArray(Cube.all) ? Cube.all.length : 0;
        const meshCount = typeof Mesh !== "undefined" && Array.isArray(Mesh.all) ? Mesh.all.length : 0;

        if (cubeCount === 0 && meshCount === 0) {
            warnings.push("Aucune géométrie trouvée (ni cubes ni meshes).");
        }

        Animation.all.forEach(function (animation) {
            const animators = animation && animation.animators ? animation.animators : {};

            Object.keys(animators).forEach(function (animatorId) {
                const animator = animators[animatorId];

                if (!animator || animator.type !== "bone") {
                    return;
                }

                if (!groupsByUuid[animatorId] && !animator.name) {
                    warnings.push("Animation '" + animation.name + "' : animateur d'os non résolu : " + animatorId);
                }

                const keyframes = Array.isArray(animator.keyframes) ? animator.keyframes : [];
                keyframes.forEach(function (keyframe) {
                    if (!normalizeChannel(keyframe.channel)) {
                        warnings.push(
                            "Animation '" + animation.name + "' utilise un canal non supporté : '" + keyframe.channel + "'"
                        );
                    }
                });
            });
        });

        return warnings;
    }

    function showValidationResult() {
        const warnings = projectValidationWarnings();

        if (warnings.length === 0) {
            Blockbench.showQuickMessage("Validation AnimaTruc OK", 2500);
            return;
        }

        Blockbench.showMessageBox({
            title: "Validation AnimaTruc",
            message: warnings.join("\n")
        });
    }

    function showPackPreview() {
        const pack = buildPack();
        const preview = [
            "Projet : " + (pack.meta.projectName || "sans_nom"),
            "Os : " + pack.skeleton.bones.length,
            "Cubes : " + pack.model.cubes.length,
            "Meshes : " + (pack.model.meshes ? pack.model.meshes.length : 0),
            "Animations : " + pack.clips.length
        ];

        if (pack.clips.length > 0) {
            preview.push("Noms des animations : " + pack.clips.map(function (clip) { return clip.name; }).join(", "));
        }

        Blockbench.showMessageBox({
            title: "Aperçu du pack AnimaTruc",
            message: preview.join("\n")
        });
    }

    function exportPack() {
        if (!Project) {
            Blockbench.showQuickMessage("Aucun projet Blockbench actif.", 3000);
            return;
        }

        const pack = buildPack();
        const suggestedName = sanitizeBaseFileName(Project.name || "animatruc_project");

        Blockbench.export({
            resource_id: "animatruc_pack",
            type: "AnimaTruc",
            extensions: ["animatrucpack"],
            name: suggestedName,
            content: JSON.stringify(pack, null, 2)
        });
    }

    function triggerNativeAction(actionId, fallbackMessage) {
        if (typeof BarItems !== "undefined" && BarItems[actionId] && typeof BarItems[actionId].trigger === "function") {
            BarItems[actionId].trigger();
            return true;
        }

        Blockbench.showMessageBox({
            title: "AnimaTruc",
            message: fallbackMessage
        });
        return false;
    }

    function openBbmodelForEditing() {
        triggerNativeAction(
            "open_model",
            "Action native 'ouvrir un modèle' introuvable. Utilise Fichier -> Ouvrir un modèle puis choisis un .bbmodel."
        );
    }

    function importGltfForEditing() {
        if (triggerNativeAction("import_gltf", "")) {
            return;
        }

        triggerNativeAction(
            "open_model",
            "Action native 'importer glTF' introuvable. Utilise Fichier -> Importer -> glTF ou ouvre le fichier manuellement."
        );
    }

    Plugin.register(PLUGIN_ID, {
        title: "AnimaTruc Exportateur",
        author: "Hoyatla",
        icon: "icon-animation",
        description: "Ouvre des modèles source, prévisualise et exporte des packs runtime AnimaTruc unifiés (modèle + animations).",
        version: "2.0.0",
        variant: "both",
        onload: function () {
            openBbmodelAction = new Action("animatruc_open_bbmodel", {
                name: "Ouvrir .bbmodel pour AnimaTruc",
                description: "Ouvre un modèle Blockbench pour édition avant export AnimaTruc",
                icon: "folder_open",
                click: openBbmodelForEditing
            });

            importGltfAction = new Action("animatruc_import_gltf", {
                name: "Importer .gltf pour AnimaTruc",
                description: "Importe un modèle glTF pour édition avant export AnimaTruc",
                icon: "insert_drive_file",
                click: importGltfForEditing
            });

            validateAction = new Action("animatruc_validate_project", {
                name: "Valider pour AnimaTruc",
                description: "Valide squelette, canaux et géométrie avant export",
                icon: "check_circle",
                click: showValidationResult
            });

            previewAction = new Action("animatruc_preview_pack", {
                name: "Prévisualiser le pack AnimaTruc",
                description: "Affiche les compteurs du pack et les noms des animations",
                icon: "visibility",
                click: showPackPreview
            });

            exportAction = new Action("animatruc_export_pack", {
                name: "Exporter AnimaTruc",
                description: "Exporte modèle + animations vers .animatrucpack",
                icon: "fa-file-export",
                click: exportPack
            });

            MenuBar.addAction(openBbmodelAction, "file");
            MenuBar.addAction(importGltfAction, "file.import");
            MenuBar.addAction(validateAction, "animation");
            MenuBar.addAction(previewAction, "animation");
            MenuBar.addAction(exportAction, "file.export");
        },
        onunload: function () {
            const actions = [openBbmodelAction, importGltfAction, validateAction, previewAction, exportAction];
            actions.forEach(function (action) {
                if (action && typeof action.delete === "function") {
                    action.delete();
                }
            });

            openBbmodelAction = null;
            importGltfAction = null;
            validateAction = null;
            previewAction = null;
            exportAction = null;
        }
    });
})();
