(function () {
    const PLUGIN_ID = "animatruc_exporter";
    const TPS = 20;

    let exportAction = null;
    let validateAction = null;

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

    function quaternionFromEulerDegrees(pitch, yaw, roll) {
        const degToRad = Math.PI / 180;
        const qx = axisAngleQuat(1, 0, 0, pitch * degToRad);
        const qy = axisAngleQuat(0, 1, 0, yaw * degToRad);
        const qz = axisAngleQuat(0, 0, 1, roll * degToRad);

        return normalizeQuat(multiplyQuat(multiplyQuat(qy, qx), qz));
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
        const len = Math.sqrt(
            quat[0] * quat[0] +
            quat[1] * quat[1] +
            quat[2] * quat[2] +
            quat[3] * quat[3]
        );

        if (len <= 0) {
            return [0, 0, 0, 1];
        }

        return [quat[0] / len, quat[1] / len, quat[2] / len, quat[3] / len];
    }

    function normalizeChannel(channel) {
        const normalized = String(channel || "").trim().toLowerCase();

        if (normalized === "position" || normalized === "translation" || normalized === "location") {
            return "translation";
        }
        if (normalized === "rotation") {
            return "rotation";
        }
        if (normalized === "scale") {
            return "scale";
        }

        return null;
    }

    function normalizeInterpolation(interpolation) {
        const value = String(interpolation || "LINEAR").trim().toUpperCase();
        return value === "STEP" ? "STEP" : "LINEAR";
    }

    function resolveLooping(loopValue) {
        if (typeof loopValue === "boolean") {
            return loopValue;
        }

        const normalized = String(loopValue || "loop").trim().toLowerCase();

        if (normalized === "once" || normalized === "hold" || normalized === "hold_on_last_frame") {
            return false;
        }

        return true;
    }

    function vec3Array(values, fallbackX, fallbackY, fallbackZ) {
        if (!Array.isArray(values)) {
            return [fallbackX, fallbackY, fallbackZ];
        }

        return [
            toNumber(values[0], fallbackX),
            toNumber(values[1], fallbackY),
            toNumber(values[2], fallbackZ)
        ];
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
            const origin = vec3Array(group.origin, 0, 0, 0);
            const rotationEuler = vec3Array(group.rotation, 0, 0, 0);
            const scale = vec3Array(group.scale, 1, 1, 1);

            return {
                name: group.name,
                parent: parent,
                pivot: origin,
                bindPose: {
                    translation: [0, 0, 0],
                    rotation: quaternionFromEulerDegrees(rotationEuler[0], rotationEuler[1], rotationEuler[2]),
                    scale: scale
                }
            };
        });
    }

    function buildClipTrack(animator) {
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

    function hasTrackData(track) {
        return track.translation.length > 0 || track.rotation.length > 0 || track.scale.length > 0;
    }

    function buildClips() {
        const groupsByUuid = {};
        Group.all.forEach(function (group) {
            groupsByUuid[group.uuid] = group;
        });

        return Animation.all.map(function (animation) {
            const tracks = {};
            const animators = animation && animation.animators ? animation.animators : {};

            Object.keys(animators).forEach(function (animatorId) {
                const animator = animators[animatorId];

                if (!animator || animator.type !== "bone") {
                    return;
                }

                const group = groupsByUuid[animatorId];
                const boneName = group ? group.name : (animator.name || animatorId);
                const track = buildClipTrack(animator);

                if (hasTrackData(track)) {
                    tracks[boneName] = track;
                }
            });

            return {
                name: animation.name || "clip",
                lengthTicks: Math.max(0, toNumber(animation.length, 0) * TPS),
                looping: resolveLooping(animation.loop),
                tracks: tracks
            };
        });
    }

    function buildPack() {
        const projectName = Project && Project.name ? Project.name : "animatruc_pack";

        return {
            format: "animatruc-pack",
            version: 1,
            meta: {
                source: "blockbench",
                plugin: "animatruc_exporter",
                projectName: projectName,
                exportedAt: new Date().toISOString()
            },
            skeleton: {
                bones: buildSkeleton()
            },
            clips: buildClips()
        };
    }

    function validateProject() {
        const warnings = [];
        const nameSet = {};
        const groupByUuid = {};

        Group.all.forEach(function (group) {
            if (nameSet[group.name]) {
                warnings.push("Duplicate bone name detected: " + group.name);
            }
            nameSet[group.name] = true;
            groupByUuid[group.uuid] = group;
        });

        if (Group.all.length === 0) {
            warnings.push("No bones found. Create at least one Group.");
        }

        if (Animation.all.length === 0) {
            warnings.push("No animations found.");
        }

        Animation.all.forEach(function (animation) {
            const animators = animation && animation.animators ? animation.animators : {};

            Object.keys(animators).forEach(function (animatorId) {
                const animator = animators[animatorId];

                if (!animator || animator.type !== "bone") {
                    return;
                }

                if (!groupByUuid[animatorId] && !animator.name) {
                    warnings.push("Animation '" + animation.name + "' contains a bone animator without resolvable bone: " + animatorId);
                }

                const keyframes = Array.isArray(animator.keyframes) ? animator.keyframes : [];
                keyframes.forEach(function (keyframe) {
                    if (!normalizeChannel(keyframe.channel)) {
                        warnings.push(
                            "Animation '" + animation.name + "' uses unsupported channel '" +
                            keyframe.channel +
                            "' on animator '" + animatorId + "'."
                        );
                    }
                });
            });
        });

        if (warnings.length === 0) {
            Blockbench.showQuickMessage("AnimaTruc: validation OK", 3000);
            return;
        }

        Blockbench.showMessageBox({
            title: "AnimaTruc Validation",
            message: warnings.join("\n")
        });
    }

    function exportPack() {
        if (!Project) {
            Blockbench.showQuickMessage("No active Blockbench project.", 3000);
            return;
        }

        const pack = buildPack();
        const suggestedName = (Project.name || "animatruc_pack") + ".animatruc";

        Blockbench.export({
            resource_id: "animatruc_pack",
            type: "AnimaTruc Pack",
            extensions: ["animatruc.json"],
            name: suggestedName,
            content: JSON.stringify(pack, null, 2)
        });
    }

    Plugin.register(PLUGIN_ID, {
        title: "AnimaTruc Exporter",
        author: "Hoyatla",
        icon: "icon-animation",
        description: "Export Blockbench skeleton and animations directly to AnimaTruc pack format.",
        version: "1.0.0",
        variant: "both",
        onload: function () {
            exportAction = new Action("animatruc_export_pack", {
                name: "Export AnimaTruc Pack",
                description: "Export current project to .animatruc.json",
                icon: "fa-file-export",
                click: exportPack
            });

            validateAction = new Action("animatruc_validate_project", {
                name: "Validate For AnimaTruc",
                description: "Validate skeleton and animation channels before export",
                icon: "check_circle",
                click: validateProject
            });

            MenuBar.addAction(exportAction, "file.export");
            MenuBar.addAction(validateAction, "animation");
        },
        onunload: function () {
            if (exportAction) {
                exportAction.delete();
                exportAction = null;
            }
            if (validateAction) {
                validateAction.delete();
                validateAction = null;
            }
        }
    });
})();
