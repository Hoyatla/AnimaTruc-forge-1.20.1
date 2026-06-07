(function () {
    const PLUGIN_ID = "animatruc_exporter";
    const PACK_VERSION = 2;
    const TPS = 20;

    let exportAction = null;
    let exportJsonAction = null;
    let importAnimatrucAction = null;
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

    function toInteger(value, fallback) {
        if (typeof value === "number" && Number.isInteger(value)) {
            return value;
        }

        if (typeof value === "string") {
            const parsed = Number.parseInt(value, 10);

            if (Number.isInteger(parsed)) {
                return parsed;
            }
        }

        return fallback;
    }

    function toVec2(values, fallbackX, fallbackY) {
        if (!Array.isArray(values)) {
            return [fallbackX, fallbackY];
        }

        return [
            toNumber(values[0], fallbackX),
            toNumber(values[1], fallbackY)
        ];
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

    function safeArray(value) {
        return Array.isArray(value) ? value : [];
    }

    function safeObject(value) {
        return value && typeof value === "object" && !Array.isArray(value) ? value : {};
    }

    function clamp(value, min, max) {
        return Math.min(Math.max(value, min), max);
    }

    function radiansToDegrees(value) {
        return value * 180 / Math.PI;
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

    function quaternionToEulerDegrees(quatValues) {
        const quat = Array.isArray(quatValues)
            ? [
                toNumber(quatValues[0], 0),
                toNumber(quatValues[1], 0),
                toNumber(quatValues[2], 0),
                toNumber(quatValues[3], 1)
            ]
            : [0, 0, 0, 1];
        const normalized = normalizeQuat(quat);

        if (typeof THREE !== "undefined" && THREE && typeof THREE.Quaternion === "function" && typeof THREE.Euler === "function") {
            const threeQuat = new THREE.Quaternion(normalized[0], normalized[1], normalized[2], normalized[3]);
            const euler = new THREE.Euler().setFromQuaternion(threeQuat, "YXZ");
            return [
                radiansToDegrees(euler.x),
                radiansToDegrees(euler.y),
                radiansToDegrees(euler.z)
            ];
        }

        const x = normalized[0];
        const y = normalized[1];
        const z = normalized[2];
        const w = normalized[3];
        const sinX = 2 * (w * x - y * z);
        const cosX = 1 - 2 * (x * x + y * y);
        const sinY = 2 * (w * y + x * z);
        const sinZ = 2 * (w * z - x * y);
        const cosZ = 1 - 2 * (y * y + z * z);

        return [
            radiansToDegrees(Math.atan2(sinX, cosX)),
            radiansToDegrees(Math.asin(clamp(sinY, -1, 1))),
            radiansToDegrees(Math.atan2(sinZ, cosZ))
        ];
    }

    function unwrapEulerDegrees(current, previous) {
        if (!Array.isArray(previous)) {
            return current.slice();
        }

        const unwrapped = current.slice();

        for (let axis = 0; axis < 3; axis++) {
            while (unwrapped[axis] - previous[axis] > 180) {
                unwrapped[axis] -= 360;
            }

            while (unwrapped[axis] - previous[axis] < -180) {
                unwrapped[axis] += 360;
            }
        }

        return unwrapped;
    }

    function normalizeInterpolation(value) {
        const normalized = String(value || "LINEAR").trim().toUpperCase();
        return normalized === "STEP" ? "STEP" : "LINEAR";
    }

    function normalizeInterpolationForBlockbench(value) {
        return normalizeInterpolation(value) === "STEP" ? "step" : "linear";
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

        const suffixes = [
            ".animatrucpack.json",
            ".animatrucpack",
            ".animatruc.json",
            ".animatruc",
            ".json"
        ];
        let stripped = name;
        let changed = true;

        while (changed && stripped.length > 0) {
            changed = false;
            const lowered = stripped.toLowerCase();
            suffixes.forEach(function (suffix) {
                if (!changed && lowered.endsWith(suffix)) {
                    stripped = stripped.slice(0, stripped.length - suffix.length);
                    changed = true;
                }
            });
        }

        name = stripped
            .replace(/[<>:"/\\|?*\x00-\x1F]/g, "_")
            .replace(/\s+/g, "_");

        return name.length > 0 ? name : "animatruc_project";
    }

    function isAnimatrucCandidateFile(fileName) {
        const normalized = String(fileName || "").trim().toLowerCase();
        return normalized.endsWith(".animatruc")
            || normalized.endsWith(".animatruc.json")
            || normalized.endsWith(".animatrucpack")
            || normalized.endsWith(".animatrucpack.json")
            || normalized.endsWith(".json");
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

    function extractRotationEuler(rotationValue) {
        if (Array.isArray(rotationValue)) {
            if (rotationValue.length >= 4) {
                return quaternionToEulerDegrees(rotationValue);
            }

            if (rotationValue.length >= 3) {
                return toVec3(rotationValue, 0, 0, 0);
            }
        }

        const objectValue = safeObject(rotationValue);

        if (Object.prototype.hasOwnProperty.call(objectValue, "w")) {
            return quaternionToEulerDegrees([
                toNumber(objectValue.x, 0),
                toNumber(objectValue.y, 0),
                toNumber(objectValue.z, 0),
                toNumber(objectValue.w, 1)
            ]);
        }

        if (Object.prototype.hasOwnProperty.call(objectValue, "x")
            || Object.prototype.hasOwnProperty.call(objectValue, "y")
            || Object.prototype.hasOwnProperty.call(objectValue, "z")) {
            return [
                toNumber(objectValue.x, 0),
                toNumber(objectValue.y, 0),
                toNumber(objectValue.z, 0)
            ];
        }

        return [0, 0, 0];
    }

    function hasThreeMatrixRuntime() {
        return typeof THREE !== "undefined"
            && THREE
            && typeof THREE.Matrix4 === "function"
            && typeof THREE.Quaternion === "function"
            && typeof THREE.Vector3 === "function";
    }

    function composeMatrix(translation, rotation, scale) {
        if (!hasThreeMatrixRuntime()) {
            return null;
        }

        const matrix = new THREE.Matrix4();
        matrix.compose(
            new THREE.Vector3(translation[0], translation[1], translation[2]),
            new THREE.Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]),
            new THREE.Vector3(scale[0], scale[1], scale[2])
        );
        return matrix;
    }

    function subtractVec3(a, b) {
        return [
            toNumber(a[0], 0) - toNumber(b[0], 0),
            toNumber(a[1], 0) - toNumber(b[1], 0),
            toNumber(a[2], 0) - toNumber(b[2], 0)
        ];
    }

    function transformVertexByInverseMatrix(vertex, inverseMatrix) {
        if (!inverseMatrix || !hasThreeMatrixRuntime()) {
            return vertex.slice();
        }

        const vec = new THREE.Vector3(vertex[0], vertex[1], vertex[2]);
        vec.applyMatrix4(inverseMatrix);
        return [vec.x, vec.y, vec.z];
    }

    function buildBoneBindMatrices(pack) {
        const result = {
            inverseByName: {}
        };

        if (!hasThreeMatrixRuntime()) {
            return result;
        }

        const skeletonBones = safeArray(safeObject(pack.skeleton).bones);
        const bonesByName = {};
        const worldByName = {};

        skeletonBones.forEach(function (bone) {
            const safeBone = safeObject(bone);
            const name = safeBone.name ? String(safeBone.name).trim() : "";
            if (name) {
                bonesByName[name] = safeBone;
            }
        });

        function resolveWorld(name) {
            if (worldByName[name]) {
                return worldByName[name];
            }

            const bone = bonesByName[name];
            if (!bone) {
                return null;
            }

            const bindPose = safeObject(bone.bindPose);
            const pivot = toVec3(bone.pivot, 0, 0, 0);
            const rotation = Array.isArray(bindPose.rotation)
                ? [
                    toNumber(bindPose.rotation[0], 0),
                    toNumber(bindPose.rotation[1], 0),
                    toNumber(bindPose.rotation[2], 0),
                    toNumber(bindPose.rotation[3], 1)
                ]
                : [0, 0, 0, 1];
            const scale = toVec3(bindPose.scale, 1, 1, 1);
            let translation = pivot;

            if (bone.parent && bonesByName[bone.parent]) {
                const parentPivot = toVec3(bonesByName[bone.parent].pivot, 0, 0, 0);
                translation = subtractVec3(pivot, parentPivot);
            }

            const localMatrix = composeMatrix(translation, rotation, scale);
            if (!localMatrix) {
                return null;
            }

            let worldMatrix = localMatrix.clone();
            if (bone.parent && bonesByName[bone.parent]) {
                const parentWorld = resolveWorld(bone.parent);
                if (parentWorld) {
                    worldMatrix = parentWorld.clone().multiply(localMatrix);
                }
            }

            worldByName[name] = worldMatrix;
            result.inverseByName[name] = worldMatrix.clone().invert();
            return worldMatrix;
        }

        Object.keys(bonesByName).forEach(function (name) {
            resolveWorld(name);
        });

        return result;
    }

    function resolveSingleSkinBone(mesh) {
        const safeMesh = safeObject(mesh);
        const skin = safeObject(safeMesh.skin);
        const influences = safeArray(skin.influences);
        let resolvedBone = null;

        if (influences.length === 0) {
            return null;
        }

        for (let vertexIndex = 0; vertexIndex < influences.length; vertexIndex++) {
            const vertexInfluences = safeArray(influences[vertexIndex]).filter(function (influence) {
                const safeInfluence = safeObject(influence);
                return toNumber(safeInfluence.weight, 0) > 0;
            });

            if (vertexInfluences.length === 0) {
                continue;
            }

            const uniqueBones = {};
            vertexInfluences.forEach(function (influence) {
                const boneName = safeObject(influence).bone ? String(safeObject(influence).bone).trim() : "";
                if (boneName) {
                    uniqueBones[boneName] = true;
                }
            });

            const names = Object.keys(uniqueBones);
            if (names.length !== 1) {
                return null;
            }

            if (resolvedBone === null) {
                resolvedBone = names[0];
            }
            else if (resolvedBone !== names[0]) {
                return null;
            }
        }

        return resolvedBone;
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

    function textureFilePath(texture) {
        if (!texture || typeof texture !== "object") {
            return "";
        }

        return String(texture.path || texture.source || texture.saved_path || texture.saved_name || "").trim();
    }

    function textureDisplayName(texture, index) {
        const rawName = texture && (texture.name || texture.id || texture.saved_name || texture.uuid || ("texture_" + index));
        return sanitizeBaseFileName(rawName);
    }

    function buildTextureCatalog() {
        const textures = [];
        const materials = [];
        const textureNameByRef = {};
        const materialNameByTextureName = {};

        if (typeof Texture === "undefined" || !Array.isArray(Texture.all)) {
            return {
                textures: textures,
                materials: materials,
                textureNameByRef: textureNameByRef,
                materialNameByTextureName: materialNameByTextureName
            };
        }

        Texture.all.forEach(function (texture, index) {
            if (!texture) {
                return;
            }

            const textureName = textureDisplayName(texture, index);
            const materialName = "mat_" + textureName;
            const width = toInteger(texture.width || texture.uv_width || (Project && Project.texture_width), 0);
            const height = toInteger(texture.height || texture.uv_height || (Project && Project.texture_height), 0);
            const path = textureFilePath(texture);

            textures.push({
                name: textureName,
                path: path,
                width: width,
                height: height
            });
            materials.push({
                name: materialName,
                texture: textureName,
                renderType: "entityCutoutNoCull"
            });

            materialNameByTextureName[textureName] = materialName;
            textureNameByRef[String(index)] = textureName;
            textureNameByRef[textureName] = textureName;

            if (texture.uuid) {
                textureNameByRef[String(texture.uuid)] = textureName;
            }
            if (texture.id !== undefined && texture.id !== null) {
                textureNameByRef[String(texture.id)] = textureName;
            }
            if (texture.name) {
                textureNameByRef[String(texture.name)] = textureName;
            }
            if (texture.saved_name) {
                textureNameByRef[String(texture.saved_name)] = textureName;
            }
            if (path) {
                textureNameByRef[path] = textureName;
            }
        });

        return {
            textures: textures,
            materials: materials,
            textureNameByRef: textureNameByRef,
            materialNameByTextureName: materialNameByTextureName
        };
    }

    function resolveTextureName(textureReference, textureCatalog) {
        if (textureReference === undefined || textureReference === null || textureReference === false) {
            return null;
        }

        const normalizedReference = String(textureReference).trim();
        if (!normalizedReference || normalizedReference === "-1") {
            return null;
        }

        return textureCatalog.textureNameByRef[normalizedReference] || null;
    }

    function resolveMaterialName(textureNames, textureCatalog) {
        const unique = {};

        safeArray(textureNames).forEach(function (textureName) {
            if (textureName) {
                unique[textureName] = true;
            }
        });

        const names = Object.keys(unique);
        if (names.length !== 1) {
            return null;
        }

        return textureCatalog.materialNameByTextureName[names[0]] || null;
    }

    function buildModelCubes(textureCatalog) {
        if (!Cube || !Array.isArray(Cube.all)) {
            return [];
        }

        return Cube.all.map(function (cube, index) {
            const parent = cube.parent instanceof Group ? cube.parent.name : null;
            const cubeFaces = safeObject(cube.faces);
            const material = resolveMaterialName(Object.keys(cubeFaces).map(function (faceKey) {
                const face = safeObject(cubeFaces[faceKey]);
                return resolveTextureName(face.texture, textureCatalog);
            }), textureCatalog);
            const exported = {
                name: cube.name || ("cube_" + index),
                bone: parent || "root",
                from: toVec3(cube.from, 0, 0, 0),
                to: toVec3(cube.to, 0, 0, 0),
                inflate: toNumber(cube.inflate, 0),
                mirror: Boolean(cube.mirror_uv || cube.mirror)
            };

            if (material) {
                exported.material = material;
            }

            return exported;
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

    function buildMeshFaces(mesh, indicesByKey, textureCatalog) {
        const faces = [];
        const materialCandidates = [];

        if (!mesh || !mesh.faces || typeof mesh.faces !== "object") {
            return { faces: faces, material: null };
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

                const textureName = resolveTextureName(face.texture, textureCatalog);
                if (textureName) {
                    materialCandidates.push(textureName);
                }
            }
        });

        return {
            faces: faces,
            material: resolveMaterialName(materialCandidates, textureCatalog)
        };
    }

    function buildModelMeshes(textureCatalog) {
        if (typeof Mesh === "undefined" || !Array.isArray(Mesh.all)) {
            return [];
        }

        return Mesh.all.map(function (mesh, index) {
            const parent = mesh.parent instanceof Group ? mesh.parent.name : "root";
            const origin = toVec3(mesh.origin, 0, 0, 0);
            const parsedVertices = readMeshVertices(mesh);
            const meshFaceData = buildMeshFaces(mesh, parsedVertices.indicesByKey, textureCatalog);
            const exported = {
                name: mesh.name || ("mesh_" + index),
                bone: parent,
                origin: origin,
                vertices: parsedVertices.vertices,
                faces: meshFaceData.faces
            };

            if (meshFaceData.material) {
                exported.material = meshFaceData.material;
            }

            return exported;
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
        const textureCatalog = buildTextureCatalog();

        return {
            format: "animatruc-pack",
            version: PACK_VERSION,
            meta: {
                source: "blockbench",
                pluginId: PLUGIN_ID,
                projectName: projectName,
                exportedAt: new Date().toISOString(),
                textures: textureCatalog.textures,
                materials: textureCatalog.materials
            },
            skeleton: {
                bones: buildSkeleton()
            },
            model: {
                cubes: buildModelCubes(textureCatalog),
                meshes: buildModelMeshes(textureCatalog)
            },
            clips: buildClips()
        };
    }

    function parseAnimatrucPayload(content, fileName) {
        let parsed;

        try {
            parsed = JSON.parse(String(content || ""));
        }
        catch (error) {
            Blockbench.showMessageBox({
                title: "Import AnimaTruc",
                message: "Le fichier '" + fileName + "' n'est pas un JSON valide."
            });
            return null;
        }

        if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
            Blockbench.showMessageBox({
                title: "Import AnimaTruc",
                message: "Le fichier '" + fileName + "' ne contient pas un pack AnimaTruc valide."
            });
            return null;
        }

        const skeleton = safeObject(parsed.skeleton);
        const hasBones = Array.isArray(skeleton.bones);
        const format = String(parsed.format || "").trim().toLowerCase();
        const looksLikePack = format === "animatruc-pack" || hasBones || !!parsed.model || Array.isArray(parsed.clips);

        if (!looksLikePack || !hasBones) {
            Blockbench.showMessageBox({
                title: "Import AnimaTruc",
                message: "Le fichier '" + fileName + "' n'est pas reconnu comme pack AnimaTruc."
            });
            return null;
        }

        parsed.model = safeObject(parsed.model);
        parsed.clips = safeArray(parsed.clips);
        return parsed;
    }

    function clearCurrentProject() {
        if (Array.isArray(Animation.all)) {
            Animation.all.slice().forEach(function (animation) {
                if (animation && typeof animation.remove === "function") {
                    animation.remove(false, true);
                }
            });
        }

        if (typeof Outliner !== "undefined" && Array.isArray(Outliner.root)) {
            Outliner.root.slice().forEach(function (node) {
                if (node && typeof node.remove === "function") {
                    node.remove();
                }
            });
        }
    }

    function ensureProjectAvailable() {
        if (Project) {
            return true;
        }

        if (typeof newProject === "function" && typeof Formats !== "undefined") {
            const fallbackFormat = Formats.free || Formats.generic || Formats.modded_entity || Formats.bedrock;

            if (fallbackFormat) {
                newProject(fallbackFormat);
            }
        }

        if (Project) {
            return true;
        }

        Blockbench.showMessageBox({
            title: "Import AnimaTruc",
            message: "Aucun projet Blockbench actif. Cree d'abord un projet capable de contenir des groupes, meshes et animations."
        });
        return false;
    }

    function collectReferencedBoneNames(pack) {
        const names = {};
        const skeletonBones = safeArray(safeObject(pack.skeleton).bones);
        const cubes = safeArray(safeObject(pack.model).cubes);
        const meshes = safeArray(safeObject(pack.model).meshes);
        const clips = safeArray(pack.clips);

        skeletonBones.forEach(function (bone) {
            const name = bone && bone.name ? String(bone.name).trim() : "";

            if (name) {
                names[name] = true;
            }
        });

        cubes.forEach(function (cube) {
            const boneName = cube && cube.bone ? String(cube.bone).trim() : "";

            if (boneName) {
                names[boneName] = true;
            }
        });

        meshes.forEach(function (mesh) {
            const boneName = mesh && mesh.bone ? String(mesh.bone).trim() : "";

            if (boneName) {
                names[boneName] = true;
            }
        });

        clips.forEach(function (clip) {
            const tracks = safeObject(clip && clip.tracks);
            Object.keys(tracks).forEach(function (boneName) {
                if (boneName) {
                    names[boneName] = true;
                }
            });
        });

        return Object.keys(names);
    }

    function createGroupNode(bone, parentGroup) {
        const bindPose = safeObject(bone.bindPose);
        const group = new Group({
            name: bone.name,
            origin: toVec3(bone.pivot, 0, 0, 0),
            rotation: extractRotationEuler(bindPose.rotation)
        }).addTo(parentGroup || "root").init();

        if (bindPose.scale) {
            group.scale = toVec3(bindPose.scale, 1, 1, 1);
        }

        return group;
    }

    function importSkeleton(pack) {
        const skeleton = safeObject(pack.skeleton);
        const sourceBones = safeArray(skeleton.bones);
        const bonesByName = {};

        sourceBones.forEach(function (bone) {
            const safeBone = safeObject(bone);
            const name = safeBone.name ? String(safeBone.name).trim() : "";

            if (!name) {
                return;
            }

            bonesByName[name] = {
                name: name,
                parent: safeBone.parent ? String(safeBone.parent).trim() : null,
                pivot: toVec3(safeBone.pivot, 0, 0, 0),
                bindPose: safeObject(safeBone.bindPose)
            };
        });

        collectReferencedBoneNames(pack).forEach(function (boneName) {
            if (!bonesByName[boneName]) {
                bonesByName[boneName] = {
                    name: boneName,
                    parent: null,
                    pivot: [0, 0, 0],
                    bindPose: {
                        translation: [0, 0, 0],
                        rotation: [0, 0, 0, 1],
                        scale: [1, 1, 1]
                    }
                };
            }
        });

        const groupsByName = {};
        const pending = Object.keys(bonesByName).map(function (name) { return bonesByName[name]; });
        let guard = pending.length * pending.length + 8;

        while (pending.length > 0 && guard-- > 0) {
            let progressed = false;

            for (let index = pending.length - 1; index >= 0; index--) {
                const bone = pending[index];
                const parentName = bone.parent;

                if (!parentName || groupsByName[parentName]) {
                    groupsByName[bone.name] = createGroupNode(bone, parentName ? groupsByName[parentName] : null);
                    pending.splice(index, 1);
                    progressed = true;
                }
            }

            if (!progressed) {
                pending.forEach(function (bone) {
                    if (!groupsByName[bone.name]) {
                        const detachedBone = {
                            name: bone.name,
                            parent: null,
                            pivot: bone.pivot,
                            bindPose: bone.bindPose
                        };
                        groupsByName[bone.name] = createGroupNode(detachedBone, null);
                    }
                });
                pending.length = 0;
            }
        }

        return groupsByName;
    }

    function importCubes(pack, groupsByName) {
        const cubes = safeArray(safeObject(pack.model).cubes);

        cubes.forEach(function (cube, index) {
            const safeCube = safeObject(cube);
            const boneName = safeCube.bone ? String(safeCube.bone).trim() : "root";
            const parentGroup = groupsByName[boneName] || "root";
            const cubeNode = new Cube({
                name: safeCube.name || ("cube_" + index),
                from: toVec3(safeCube.from, 0, 0, 0),
                to: toVec3(safeCube.to, 0, 0, 0),
                inflate: toNumber(safeCube.inflate, 0),
                mirror_uv: Boolean(safeCube.mirror),
                origin: parentGroup instanceof Group ? toVec3(parentGroup.origin, 0, 0, 0) : [0, 0, 0]
            }).addTo(parentGroup).init();

            if (safeCube.mirror) {
                cubeNode.mirror_uv = true;
            }
        });
    }

    function importMeshes(pack, groupsByName) {
        if (typeof Mesh === "undefined" || typeof MeshFace === "undefined") {
            Blockbench.showQuickMessage("Import mesh ignore: API Mesh indisponible.", 3000);
            return;
        }

        const meshes = safeArray(safeObject(pack.model).meshes);
        const bindMatrices = buildBoneBindMatrices(pack);
        let flattenedSkinCount = 0;

        meshes.forEach(function (mesh, index) {
            const safeMesh = safeObject(mesh);
            const declaredBoneName = safeMesh.bone ? String(safeMesh.bone).trim() : "root";
            const vertices = safeArray(safeMesh.vertices).map(function (vertex) {
                return toVec3(vertex, 0, 0, 0);
            });
            const faces = safeArray(safeMesh.faces);
            const singleSkinBone = resolveSingleSkinBone(safeMesh);
            let parentGroup = groupsByName[declaredBoneName] || "root";
            let origin = toVec3(safeMesh.origin, 0, 0, 0);
            let importedVertices = vertices;

            if (vertices.length === 0 || faces.length === 0) {
                return;
            }

            if (singleSkinBone && bindMatrices.inverseByName[singleSkinBone]) {
                parentGroup = groupsByName[singleSkinBone] || "root";
                origin = [0, 0, 0];
                importedVertices = vertices.map(function (vertex) {
                    return transformVertexByInverseMatrix(vertex, bindMatrices.inverseByName[singleSkinBone]);
                });
            }
            else if (safeMesh.skin) {
                parentGroup = "root";
                origin = [0, 0, 0];
                flattenedSkinCount++;
            }

            const meshNode = new Mesh({
                name: safeMesh.name || ("mesh_" + index),
                origin: origin
            }).addTo(parentGroup).init();
            const vertexKeys = meshNode.addVertices.apply(meshNode, importedVertices);
            const meshFaces = [];

            faces.forEach(function (face) {
                const safeFace = safeObject(face);
                const faceIndices = safeArray(safeFace.indices)
                    .map(function (value) { return toInteger(value, -1); })
                    .filter(function (value) { return value >= 0 && value < vertexKeys.length; });

                if (faceIndices.length < 3) {
                    return;
                }

                const faceVertexKeys = [];
                const uvByVertex = {};
                const faceUvs = safeArray(safeFace.uvs);

                faceIndices.forEach(function (vertexIndex, order) {
                    const vertexKey = vertexKeys[vertexIndex];

                    if (typeof vertexKey !== "string") {
                        return;
                    }

                    faceVertexKeys.push(vertexKey);

                    if (order < faceUvs.length) {
                        uvByVertex[vertexKey] = toVec2(faceUvs[order], 0, 0);
                    }
                });

                if (faceVertexKeys.length < 3) {
                    return;
                }

                const faceOptions = {
                    vertices: faceVertexKeys
                };

                if (Object.keys(uvByVertex).length === faceVertexKeys.length) {
                    faceOptions.uv = uvByVertex;
                }

                meshFaces.push(new MeshFace(meshNode, faceOptions));
            });

            if (meshFaces.length > 0) {
                meshNode.addFaces.apply(meshNode, meshFaces);
                if (typeof meshNode.updateElement === "function") {
                    meshNode.updateElement();
                }
            }
            else if (typeof meshNode.remove === "function") {
                meshNode.remove();
            }
        });

        if (flattenedSkinCount > 0) {
            Blockbench.showQuickMessage(
                "Import AnimaTruc: " + flattenedSkinCount + " mesh(s) skinnes ont ete importes en espace modele.",
                4000
            );
        }
    }

    function addAnimatorKeyframes(animator, channel, keyframes, state) {
        if (!animator || typeof animator.addKeyframe !== "function") {
            return;
        }

        safeArray(keyframes).forEach(function (keyframe) {
            const safeKeyframe = safeObject(keyframe);
            const time = Math.max(0, toNumber(safeKeyframe.tick, toNumber(safeKeyframe.time, 0))) / TPS;
            const interpolation = normalizeInterpolationForBlockbench(safeKeyframe.interpolation);
            let value;
            let blockbenchChannel;

            if (channel === "rotation") {
                value = unwrapEulerDegrees(extractRotationEuler(safeKeyframe.value), state.rotation);
                state.rotation = value.slice();
                blockbenchChannel = "rotation";
            }
            else if (channel === "translation") {
                value = toVec3(safeKeyframe.value, 0, 0, 0);
                blockbenchChannel = "position";
            }
            else {
                value = toVec3(safeKeyframe.value, 1, 1, 1);
                blockbenchChannel = "scale";
            }

            animator.addKeyframe({
                channel: blockbenchChannel,
                time: time,
                interpolation: interpolation,
                data_points: [{
                    x: value[0],
                    y: value[1],
                    z: value[2]
                }]
            });
        });
    }

    function importAnimations(pack, groupsByName) {
        const clips = safeArray(pack.clips);

        clips.forEach(function (clip, index) {
            const safeClip = safeObject(clip);
            const lengthTicks = Math.max(0, toNumber(safeClip.lengthTicks, toNumber(safeClip.lengthSeconds, 0) * TPS));
            const animation = new Animation({
                name: safeClip.name || ("clip_" + index),
                length: lengthTicks / TPS,
                loop: safeClip.looping === false ? "once" : "loop",
                snapping: TPS
            }).add(false);
            const tracks = safeObject(safeClip.tracks);

            Object.keys(tracks).forEach(function (boneName) {
                const group = groupsByName[boneName];

                if (!group) {
                    return;
                }

                const track = safeObject(tracks[boneName]);
                const animator = animation.getBoneAnimator(group);
                const state = {
                    rotation: null
                };

                addAnimatorKeyframes(animator, "translation", track.translation, state);
                addAnimatorKeyframes(animator, "rotation", track.rotation, state);
                addAnimatorKeyframes(animator, "scale", track.scale, state);
            });

            if (typeof animation.calculateSnappingFromKeyframes === "function") {
                animation.calculateSnappingFromKeyframes();
            }
        });
    }

    function refreshAfterImport() {
        if (typeof Canvas !== "undefined") {
            if (typeof Canvas.updateAll === "function") {
                Canvas.updateAll();
            }
            else if (typeof Canvas.updateView === "function") {
                Canvas.updateView({
                    elements: true,
                    element_aspects: {
                        transform: true,
                        geometry: true,
                        faces: true
                    }
                });
            }
        }

        if (typeof Animator !== "undefined" && typeof Animator.preview === "function") {
            Animator.preview();
        }

        if (Project) {
            Project.saved = false;
        }
    }

    function importAnimatrucPackFromFile(file) {
        if (!ensureProjectAvailable()) {
            return;
        }

        const parsedPack = parseAnimatrucPayload(file.content, file.name || "animatruc.json");

        if (!parsedPack) {
            return;
        }

        clearCurrentProject();

        if (Project) {
            Project.name = sanitizeBaseFileName(file.name || parsedPack.meta && parsedPack.meta.projectName || "animatruc_project");
        }

        const groupsByName = importSkeleton(parsedPack);
        importCubes(parsedPack, groupsByName);
        importMeshes(parsedPack, groupsByName);
        importAnimations(parsedPack, groupsByName);
        refreshAfterImport();

        Blockbench.showQuickMessage("Import AnimaTruc OK: " + (file.name || "pack"), 3000);
    }

    function projectValidationWarnings() {
        const warnings = [];
        const names = {};
        const groupsByUuid = {};

        Group.all.forEach(function (group) {
            if (names[group.name]) {
                warnings.push("Nom d'os duplique : " + group.name);
            }

            names[group.name] = true;
            groupsByUuid[group.uuid] = group;
        });

        if (Group.all.length === 0) {
            warnings.push("Aucun os trouve. Cree au moins un groupe pour le squelette AnimaTruc.");
        }

        const cubeCount = Cube && Array.isArray(Cube.all) ? Cube.all.length : 0;
        const meshCount = typeof Mesh !== "undefined" && Array.isArray(Mesh.all) ? Mesh.all.length : 0;

        if (cubeCount === 0 && meshCount === 0) {
            warnings.push("Aucune geometrie trouvee (ni cubes ni meshes).");
        }

        Animation.all.forEach(function (animation) {
            const animators = animation && animation.animators ? animation.animators : {};

            Object.keys(animators).forEach(function (animatorId) {
                const animator = animators[animatorId];

                if (!animator || animator.type !== "bone") {
                    return;
                }

                if (!groupsByUuid[animatorId] && !animator.name) {
                    warnings.push("Animation '" + animation.name + "' : animateur d'os non resolu : " + animatorId);
                }

                const keyframes = Array.isArray(animator.keyframes) ? animator.keyframes : [];
                keyframes.forEach(function (keyframe) {
                    if (!normalizeChannel(keyframe.channel)) {
                        warnings.push(
                            "Animation '" + animation.name + "' utilise un canal non supporte : '" + keyframe.channel + "'"
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
            "Animations : " + pack.clips.length,
            "Textures : " + safeArray(pack.meta.textures).length,
            "Matériaux : " + safeArray(pack.meta.materials).length
        ];

        if (pack.clips.length > 0) {
            preview.push("Noms des animations : " + pack.clips.map(function (clip) { return clip.name; }).join(", "));
        }

        Blockbench.showMessageBox({
            title: "Apercu du pack AnimaTruc",
            message: preview.join("\n")
        });
    }

    function exportPackAsLegacy() {
        if (!Project) {
            Blockbench.showQuickMessage("Aucun projet Blockbench actif.", 3000);
            return;
        }

        const pack = buildPack();
        const suggestedName = sanitizeBaseFileName(Project.name || "animatruc_project");

        Blockbench.export({
            resource_id: "animatruc_pack",
            type: "AnimaTruc Pack",
            extensions: ["animatrucpack"],
            name: suggestedName,
            content: JSON.stringify(pack, null, 2)
        });
    }

    function exportPackAsJson() {
        if (!Project) {
            Blockbench.showQuickMessage("Aucun projet Blockbench actif.", 3000);
            return;
        }

        const pack = buildPack();
        const suggestedName = sanitizeBaseFileName(Project.name || "animatruc_project");

        Blockbench.export({
            resource_id: "animatruc_json",
            type: "AnimaTruc JSON",
            extensions: ["json"],
            name: suggestedName + ".animatruc",
            content: JSON.stringify(pack, null, 2)
        });
    }

    function importAnimatrucForEditing() {
        Blockbench.import({
            resource_id: "animatruc_import",
            type: "AnimaTruc",
            extensions: ["animatrucpack", "animatruc", "json"],
            readtype: "text",
            title: "Importer un pack AnimaTruc",
            errorbox: true
        }, function (files) {
            const safeFiles = safeArray(files);

            if (safeFiles.length === 0) {
                return;
            }

            const file = safeFiles[0];

            if (!isAnimatrucCandidateFile(file.name)) {
                Blockbench.showMessageBox({
                    title: "Import AnimaTruc",
                    message: "Extension non supportee : " + file.name
                });
                return;
            }

            importAnimatrucPackFromFile(file);
        });
    }

    function triggerNativeAction(actionId, fallbackMessage) {
        if (typeof BarItems !== "undefined" && BarItems[actionId] && typeof BarItems[actionId].trigger === "function") {
            BarItems[actionId].trigger();
            return true;
        }

        if (fallbackMessage) {
            Blockbench.showMessageBox({
                title: "AnimaTruc",
                message: fallbackMessage
            });
        }
        return false;
    }

    function openBbmodelForEditing() {
        triggerNativeAction(
            "open_model",
            "Action native 'ouvrir un modele' introuvable. Utilise Fichier -> Ouvrir un modele puis choisis un .bbmodel."
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
        description: "Ouvre des modeles source, importe des packs AnimaTruc, previsualise et exporte des packs runtime unifies (modele + animations + metadonnees rendu).",
        version: "2.1.0",
        variant: "both",
        onload: function () {
            openBbmodelAction = new Action("animatruc_open_bbmodel", {
                name: "Ouvrir .bbmodel pour AnimaTruc",
                description: "Ouvre un modele Blockbench pour edition avant export AnimaTruc",
                icon: "folder_open",
                click: openBbmodelForEditing
            });

            importGltfAction = new Action("animatruc_import_gltf", {
                name: "Importer .gltf pour AnimaTruc",
                description: "Importe un modele glTF pour edition avant export AnimaTruc",
                icon: "insert_drive_file",
                click: importGltfForEditing
            });

            importAnimatrucAction = new Action("animatruc_import_pack", {
                name: "Importer AnimaTruc",
                description: "Importe un pack .animatruc.json ou .animatrucpack dans le projet actif",
                icon: "file_open",
                click: importAnimatrucForEditing
            });

            validateAction = new Action("animatruc_validate_project", {
                name: "Valider pour AnimaTruc",
                description: "Valide squelette, canaux et geometrie avant export",
                icon: "check_circle",
                click: showValidationResult
            });

            previewAction = new Action("animatruc_preview_pack", {
                name: "Previsualiser le pack AnimaTruc",
                description: "Affiche les compteurs du pack, les textures et les noms des animations",
                icon: "visibility",
                click: showPackPreview
            });

            exportAction = new Action("animatruc_export_pack", {
                name: "Exporter AnimaTruc (legacy)",
                description: "Exporte modele + animations vers .animatrucpack",
                icon: "fa-file-export",
                click: exportPackAsLegacy
            });

            exportJsonAction = new Action("animatruc_export_json", {
                name: "Exporter AnimaTruc JSON",
                description: "Exporte modele + animations vers .animatruc.json",
                icon: "fa-file-code",
                click: exportPackAsJson
            });

            MenuBar.addAction(openBbmodelAction, "file");
            MenuBar.addAction(importGltfAction, "file.import");
            MenuBar.addAction(importAnimatrucAction, "file.import");
            MenuBar.addAction(validateAction, "animation");
            MenuBar.addAction(previewAction, "animation");
            MenuBar.addAction(exportAction, "file.export");
            MenuBar.addAction(exportJsonAction, "file.export");
        },
        onunload: function () {
            const actions = [
                openBbmodelAction,
                importGltfAction,
                importAnimatrucAction,
                validateAction,
                previewAction,
                exportAction,
                exportJsonAction
            ];
            actions.forEach(function (action) {
                if (action && typeof action.delete === "function") {
                    action.delete();
                }
            });

            openBbmodelAction = null;
            importGltfAction = null;
            importAnimatrucAction = null;
            validateAction = null;
            previewAction = null;
            exportAction = null;
            exportJsonAction = null;
        }
    });
})();
