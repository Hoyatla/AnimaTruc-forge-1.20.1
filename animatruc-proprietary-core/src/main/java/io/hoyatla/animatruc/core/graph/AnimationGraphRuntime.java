package io.hoyatla.animatruc.core.graph;

import io.hoyatla.animatruc.core.animation.AnimationLayer;
import io.hoyatla.animatruc.core.animation.ClipState;
import io.hoyatla.animatruc.core.runtime.AnimatorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stateful evaluator for an {@link AnimationGraph}.
 */
public final class AnimationGraphRuntime {
    private final AnimationGraph graph;
    private final GraphParameters parameters = new GraphParameters();

    private ActiveState current;
    private BlendWindow activeBlend;

    public AnimationGraphRuntime(AnimationGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.current = createActiveState(this.graph.state(this.graph.entryStateId()));
    }

    public GraphParameters parameters() {
        return this.parameters;
    }

    public String currentStateId() {
        return this.current.definition.id();
    }

    public void forceState(String stateId) {
        AnimationGraphState target = this.graph.state(stateId);

        if (target == null)
            throw new IllegalArgumentException("Unknown graph state: " + stateId);

        this.current = createActiveState(target);
        this.activeBlend = null;
    }

    public List<AnimationLayer> update(AnimatorContext context, float deltaTicks) {
        float safeDelta = Math.max(0f, deltaTicks);

        if (this.activeBlend == null) {
            AnimationGraphTransition transition = selectTransition(context);

            if (transition != null) {
                ActiveState next = createActiveState(this.graph.state(transition.to()));

                if (transition.fadeTicks() <= 0f) {
                    this.current = next;
                    this.activeBlend = null;
                }
                else {
                    this.activeBlend = new BlendWindow(this.current, next, transition.fadeTicks());
                    this.current = next;
                }
            }
        }

        List<AnimationLayer> layers = new ArrayList<>(2);

        if (this.activeBlend != null) {
            float alpha = this.activeBlend.progress();
            float fromWeight = Math.max(0f, 1f - alpha) * this.activeBlend.from.definition.baseWeight();
            float toWeight = alpha * this.activeBlend.to.definition.baseWeight();

            layers.add(layerFor(this.activeBlend.from, fromWeight));
            layers.add(layerFor(this.activeBlend.to, toWeight));

            this.activeBlend.elapsedTicks += safeDelta;

            if (this.activeBlend.elapsedTicks >= this.activeBlend.durationTicks)
                this.activeBlend = null;
        }
        else {
            layers.add(layerFor(this.current, this.current.definition.baseWeight()));
        }

        return layers;
    }

    private AnimationGraphTransition selectTransition(AnimatorContext context) {
        List<AnimationGraphTransition> localTransitions = this.graph.transitionsFrom(this.current.definition.id());

        for (AnimationGraphTransition transition : localTransitions) {
            if (transition.condition().test(this.parameters, context))
                return transition;
        }

        List<AnimationGraphTransition> wildcardTransitions = this.graph.transitionsFrom(AnimationGraphTransition.ANY_STATE);

        for (AnimationGraphTransition transition : wildcardTransitions) {
            if (transition.condition().test(this.parameters, context))
                return transition;
        }

        return null;
    }

    private static AnimationLayer layerFor(ActiveState state, float layerWeight) {
        return AnimationLayer
                .of(state.clipState)
                .withMask(state.definition.boneMask())
                .withLayerWeight(layerWeight);
    }

    private static ActiveState createActiveState(AnimationGraphState state) {
        ClipState clipState = new ClipState(state.clip(), 1f, state.additive());

        return new ActiveState(state, clipState);
    }

    private record ActiveState(AnimationGraphState definition, ClipState clipState) {
    }

    private static final class BlendWindow {
        private final ActiveState from;
        private final ActiveState to;
        private final float durationTicks;
        private float elapsedTicks;

        private BlendWindow(ActiveState from, ActiveState to, float durationTicks) {
            this.from = from;
            this.to = to;
            this.durationTicks = Math.max(0.001f, durationTicks);
        }

        private float progress() {
            float raw = this.elapsedTicks / this.durationTicks;

            if (raw <= 0f)
                return 0f;
            if (raw >= 1f)
                return 1f;

            return raw;
        }
    }
}
