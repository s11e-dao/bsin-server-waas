package me.flyray.bsin.facade.mcp.config;

import me.flyray.bsin.facade.mcp.tools.OpenMeteoService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiMcpToolsConfig {

    @Bean
    ToolCallbackProvider tools(OpenMeteoService openMeteoService) {
        ToolCallback[] toolCallbacks = ToolCallbacks.from(openMeteoService) ;
        return ToolCallbackProvider.from(toolCallbacks) ;
    }

}
