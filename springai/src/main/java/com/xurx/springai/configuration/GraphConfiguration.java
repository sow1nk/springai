package com.xurx.springai.configuration;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
@Slf4j
public class GraphConfiguration {

    @Bean("quickGraph")
    public CompiledGraph quickGraph() throws GraphStateException {
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactory() {
            @Override
            public Map<String, KeyStrategy> apply() {
                return Map.of("input1", new ReplaceStrategy(),
                        "input2", new ReplaceStrategy());
            }
        };

        StateGraph stateGraph = new StateGraph("quickGraph", keyStrategyFactory);

        stateGraph.addNode("node1", node_async(state -> {
                log.info("node1 state: {}", state);
                return Map.of("input1", 1,
                        "input2", 1);
        }));

        stateGraph.addNode("node2", node_async(state ->{
                log.info("node2 state: {}", state);
                return Map.of("input1", 2,
                        "input2", 2);
        }));

        stateGraph.addEdge(StateGraph.START, "node1");
        stateGraph.addEdge("node1", "node2");
        stateGraph.addEdge("node2", StateGraph.END);

        return stateGraph.compile();
    }
}
