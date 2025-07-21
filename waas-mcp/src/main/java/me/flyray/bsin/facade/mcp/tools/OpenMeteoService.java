/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author brianxiadong
 */
package me.flyray.bsin.facade.mcp.tools;

import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 该API无需API密钥，可以直接使用
 */
@Service
public class OpenMeteoService {

    @Tool(description = "DID身份信息查询")
    public String didSearch(@ToolParam(description = "did") String did) {
        System.out.println("客户did：" + did);
        System.out.println("查询客户did信息成功");
        return "查询客户did信息成功";
    }

}