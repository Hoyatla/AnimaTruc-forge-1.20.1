package io.hoyatla.animatruc.core.preset;

import io.hoyatla.animatruc.core.asset.ModelBone;
import io.hoyatla.animatruc.core.asset.ModelSkeleton;
import io.hoyatla.animatruc.core.math.Vec3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Heuristic leg-chain detection from naming conventions + skeleton hierarchy + pivot layout.
 */
public final class LimbAutoDetector {
    private static final Set<String> END_TOKENS = Set.of(
            "foot", "paw", "hoof", "claw", "toe", "metatarsal", "tarsus", "legend", "leg_end"
    );
    private static final Set<String> MID_TOKENS = Set.of(
            "lowerleg", "lower_leg", "shin", "calf", "knee", "tibia", "fibia", "legmid"
    );
    private static final Set<String> ROOT_TOKENS = Set.of(
            "upperleg", "upper_leg", "thigh", "hip", "coxa", "legroot"
    );
    private static final Set<String> LEG_HINT_TOKENS = Set.of(
            "leg", "limb", "appendage", "femur"
    );
    private static final Set<String> EXCLUDED_TOKENS = Set.of(
            "arm", "hand", "finger", "wing", "tail", "head", "jaw", "mouth", "antenna", "horn", "ear"
    );
    private static final Set<String> LEFT_TOKENS = Set.of("left", "l", "lf", "fl", "port");
    private static final Set<String> RIGHT_TOKENS = Set.of("right", "r", "rf", "fr", "starboard");

    private LimbAutoDetector() {
    }

    public static LimbDetectionReport detect(ModelSkeleton skeleton) {
        return detect(skeleton, LimbDetectionOptions.DEFAULT);
    }

    public static LimbDetectionReport detect(ModelSkeleton skeleton, LimbDetectionOptions options) {
        Objects.requireNonNull(skeleton, "skeleton");
        Objects.requireNonNull(options, "options");

        if (skeleton.orderedBones().size() < 3) {
            return new LimbDetectionReport(
                    LocomotionPresetType.UNKNOWN,
                    List.of(),
                    0f,
                    false,
                    List.of("Skeleton too small for leg-chain detection")
            );
        }

        float centerX = averagePivotX(skeleton.orderedBones());
        Map<String, List<ModelBone>> childrenByParent = buildChildrenMap(skeleton.orderedBones());
        List<DetectedLimbChain> candidates = new ArrayList<>();
        int autoId = 0;

        for (ModelBone bone : skeleton.orderedBones()) {
            if (!isPotentialEndBone(bone, childrenByParent))
                continue;

            ModelBone mid = parentOf(skeleton, bone);
            ModelBone root = mid == null ? null : parentOf(skeleton, mid);

            if (mid == null || root == null)
                continue;

            Set<String> endTokens = tokens(bone.name());
            Set<String> midTokens = tokens(mid.name());
            Set<String> rootTokens = tokens(root.name());

            if (containsAny(endTokens, EXCLUDED_TOKENS)
                    || containsAny(midTokens, EXCLUDED_TOKENS)
                    || containsAny(rootTokens, EXCLUDED_TOKENS)) {
                continue;
            }

            float upperLength = distance(root.pivot(), mid.pivot());
            float lowerLength = distance(mid.pivot(), bone.pivot());

            if (upperLength < 0.01f || lowerLength < 0.01f)
                continue;

            LimbSide side = inferSide(endTokens, midTokens, rootTokens, bone.pivot().x(), centerX);
            float confidence = scoreCandidate(endTokens, midTokens, rootTokens, root, mid, bone, side, upperLength, lowerLength);

            if (confidence < options.minConfidence())
                continue;

            String id = chainId(side, ++autoId);
            candidates.add(new DetectedLimbChain(
                    id,
                    root.name(),
                    mid.name(),
                    bone.name(),
                    "ik_" + id + "_target",
                    side,
                    bone.pivot(),
                    upperLength,
                    lowerLength,
                    confidence,
                    0f
            ));
        }

        List<String> warnings = new ArrayList<>();

        if (candidates.isEmpty()) {
            return new LimbDetectionReport(
                    LocomotionPresetType.UNKNOWN,
                    List.of(),
                    0f,
                    false,
                    List.of("No reliable leg chains found (increase naming clarity or lower confidence threshold)")
            );
        }

        candidates.sort(Comparator.comparing(DetectedLimbChain::confidence).reversed());
        List<DetectedLimbChain> unique = uniqueByEndBone(candidates);

        if (unique.size() > options.maxDetectedChains()) {
            warnings.add("Detected chains exceed maxDetectedChains, trimming to " + options.maxDetectedChains());
            unique = new ArrayList<>(unique.subList(0, options.maxDetectedChains()));
        }

        LocomotionPresetType presetType = inferPreset(unique.size());

        if (!isPresetAllowed(presetType, options)) {
            warnings.add("Preset " + presetType + " disabled by options");
            return new LimbDetectionReport(presetType, List.of(), averageConfidence(unique), false, warnings);
        }

        if (presetType == LocomotionPresetType.MYRIAPOD && unique.size() > options.maxMyriapodChains()) {
            warnings.add("Myriapod chain count trimmed to maxMyriapodChains=" + options.maxMyriapodChains());
            unique = new ArrayList<>(unique.subList(0, options.maxMyriapodChains()));
        }

        unique = assignPhases(unique, presetType);
        boolean accepted = presetType != LocomotionPresetType.UNKNOWN && !unique.isEmpty();

        if (!accepted)
            warnings.add("Detected chain count does not match a supported preset");

        return new LimbDetectionReport(
                presetType,
                unique,
                averageConfidence(unique),
                accepted,
                warnings
        );
    }

    private static Map<String, List<ModelBone>> buildChildrenMap(List<ModelBone> bones) {
        Map<String, List<ModelBone>> map = new HashMap<>();

        for (ModelBone bone : bones) {
            if (bone.parentName() == null || bone.parentName().isBlank())
                continue;

            map.computeIfAbsent(bone.parentName(), ignored -> new ArrayList<>()).add(bone);
        }

        return map;
    }

    private static boolean isPotentialEndBone(ModelBone bone, Map<String, List<ModelBone>> childrenByParent) {
        Set<String> tokens = tokens(bone.name());
        boolean noChildren = !childrenByParent.containsKey(bone.name());
        boolean hasEndHint = containsAny(tokens, END_TOKENS);
        boolean hasLegHint = containsAny(tokens, LEG_HINT_TOKENS);

        if (containsAny(tokens, EXCLUDED_TOKENS))
            return false;

        return noChildren || hasEndHint || (hasLegHint && noChildren);
    }

    private static ModelBone parentOf(ModelSkeleton skeleton, ModelBone bone) {
        String parentName = bone.parentName();
        return parentName == null || parentName.isBlank() ? null : skeleton.bone(parentName);
    }

    private static float scoreCandidate(
            Set<String> endTokens,
            Set<String> midTokens,
            Set<String> rootTokens,
            ModelBone root,
            ModelBone mid,
            ModelBone end,
            LimbSide side,
            float upperLength,
            float lowerLength) {
        float score = 0f;

        score += containsAny(endTokens, END_TOKENS) ? 0.35f : 0f;
        score += containsAny(midTokens, MID_TOKENS) ? 0.25f : containsAny(midTokens, LEG_HINT_TOKENS) ? 0.10f : 0f;
        score += containsAny(rootTokens, ROOT_TOKENS) ? 0.20f : containsAny(rootTokens, LEG_HINT_TOKENS) ? 0.10f : 0f;
        score += side != LimbSide.CENTER ? 0.10f : 0f;
        score += (end.pivot().y() <= mid.pivot().y() + 0.25f && mid.pivot().y() <= root.pivot().y() + 0.25f) ? 0.10f : 0f;
        score += (upperLength > 0.05f && lowerLength > 0.05f) ? 0.10f : 0f;

        return clamp01(score);
    }

    private static List<DetectedLimbChain> uniqueByEndBone(List<DetectedLimbChain> sortedCandidates) {
        Set<String> usedEnds = new HashSet<>();
        List<DetectedLimbChain> result = new ArrayList<>(sortedCandidates.size());

        for (DetectedLimbChain chain : sortedCandidates) {
            if (!usedEnds.add(chain.endBone()))
                continue;

            result.add(chain);
        }

        return result;
    }

    private static LocomotionPresetType inferPreset(int chainCount) {
        if (chainCount == 2)
            return LocomotionPresetType.BIPED;
        if (chainCount == 6)
            return LocomotionPresetType.HEXAPOD;
        if (chainCount == 8)
            return LocomotionPresetType.OCTOPOD;
        if (chainCount > 8)
            return LocomotionPresetType.MYRIAPOD;

        return LocomotionPresetType.UNKNOWN;
    }

    private static boolean isPresetAllowed(LocomotionPresetType presetType, LimbDetectionOptions options) {
        return switch (presetType) {
            case BIPED -> options.allowBiped();
            case HEXAPOD -> options.allowHexapod();
            case OCTOPOD -> options.allowOctopod();
            case MYRIAPOD -> options.allowMyriapod();
            case UNKNOWN -> false;
        };
    }

    private static List<DetectedLimbChain> assignPhases(List<DetectedLimbChain> chains, LocomotionPresetType presetType) {
        if (chains.isEmpty())
            return List.of();

        List<DetectedLimbChain> copy = new ArrayList<>(chains);

        if (presetType == LocomotionPresetType.BIPED && copy.size() == 2) {
            DetectedLimbChain first = copy.get(0);
            DetectedLimbChain second = copy.get(1);

            if (first.side() == LimbSide.LEFT || second.side() == LimbSide.RIGHT) {
                return List.of(first.withPhaseOffset(0f), second.withPhaseOffset(0.5f));
            }

            if (first.side() == LimbSide.RIGHT || second.side() == LimbSide.LEFT) {
                return List.of(first.withPhaseOffset(0.5f), second.withPhaseOffset(0f));
            }

            return List.of(first.withPhaseOffset(0f), second.withPhaseOffset(0.5f));
        }

        copy.sort(Comparator.comparingDouble(chain -> Math.atan2(chain.anchorLocal().z(), chain.anchorLocal().x())));
        int count = copy.size();
        List<DetectedLimbChain> phased = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            phased.add(copy.get(i).withPhaseOffset((float)i / (float)count));
        }

        return Collections.unmodifiableList(phased);
    }

    private static LimbSide inferSide(
            Set<String> endTokens,
            Set<String> midTokens,
            Set<String> rootTokens,
            float pivotX,
            float centerX) {
        boolean left = containsAny(endTokens, LEFT_TOKENS) || containsAny(midTokens, LEFT_TOKENS) || containsAny(rootTokens, LEFT_TOKENS);
        boolean right = containsAny(endTokens, RIGHT_TOKENS) || containsAny(midTokens, RIGHT_TOKENS) || containsAny(rootTokens, RIGHT_TOKENS);

        if (left && !right)
            return LimbSide.LEFT;
        if (right && !left)
            return LimbSide.RIGHT;

        float localX = pivotX - centerX;

        if (localX <= -0.001f)
            return LimbSide.LEFT;
        if (localX >= 0.001f)
            return LimbSide.RIGHT;

        return LimbSide.CENTER;
    }

    private static float averagePivotX(List<ModelBone> bones) {
        if (bones.isEmpty())
            return 0f;

        float sum = 0f;

        for (ModelBone bone : bones) {
            sum += bone.pivot().x();
        }

        return sum / bones.size();
    }

    private static float averageConfidence(List<DetectedLimbChain> chains) {
        if (chains.isEmpty())
            return 0f;

        float sum = 0f;

        for (DetectedLimbChain chain : chains) {
            sum += chain.confidence();
        }

        return sum / chains.size();
    }

    private static Set<String> tokens(String value) {
        String normalized = value
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        if (normalized.isEmpty())
            return Set.of();

        String[] split = normalized.split("\\s+");
        Set<String> result = new HashSet<>(split.length * 2);

        for (String token : split) {
            if (token.isBlank())
                continue;

            result.add(token);
            if (token.length() > 1)
                result.add(token.replaceAll("[0-9]+", ""));
        }

        return result;
    }

    private static boolean containsAny(Set<String> tokens, Set<String> keywords) {
        for (String token : tokens) {
            if (keywords.contains(token))
                return true;
        }

        return false;
    }

    private static String chainId(LimbSide side, int index) {
        return switch (side) {
            case LEFT -> "left_leg_" + index;
            case RIGHT -> "right_leg_" + index;
            case CENTER -> "leg_" + index;
        };
    }

    private static float distance(Vec3f a, Vec3f b) {
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        float dz = a.z() - b.z();
        return (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float clamp01(float value) {
        if (value <= 0f)
            return 0f;
        if (value >= 1f)
            return 1f;

        return value;
    }
}
