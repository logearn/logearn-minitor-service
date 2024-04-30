package com.logearn.minitor.util;


import com.logearn.minitor.entitiy.AlarmMessage;

public class FeiShuUtils {
    public static String checkdataFail = "{\n" +
            "    \"msg_type\": \"interactive\",\n" +
            "    \"card\": {\n" +
            "        \"config\": {\n" +
            "            \"wide_screen_mode\": true\n" +
            "        },\n" +
            "        \"header\": {\n" +
            "            \"template\": \"red\",\n" +
            "            \"title\": {\n" +
            "                \"content\": \"【紧急程度 %s 级别】%s\",\n" +
            "                \"tag\": \"plain_text\"\n" +
            "            }\n" +
            "        },\n" +
            "        \"elements\": [\n" +
            "            {\n" +
            "                \"fields\": [\n" +
            "                    {\n" +
            "                        \"is_short\": true,\n" +
            "                        \"text\": {\n" +
            "                            \"content\": \"**\uD83D\uDD50 发生时间:**\\n%s\",\n" +
            "                            \"tag\": \"lark_md\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"is_short\": true,\n" +
            "                        \"text\": {\n" +
            "                            \"content\": \"**\uD83D\uDCCD 发生地点:**\\n%s\",\n" +
            "                            \"tag\": \"lark_md\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"tag\": \"div\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"tag\": \"div\",\n" +
            "                \"text\": {\n" +
            "                    \"content\": \"%s  <at id=all>\",\n" +
            "                    \"tag\": \"lark_md\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"actions\": [\n" +
            "                    {\n" +
            "                        \"tag\": \"button\",\n" +
            "                        \"text\": {\n" +
            "                            \"content\": \"%s\",\n" +
            "                            \"tag\": \"plain_text\"\n" +
            "                        },\n" +
            "                        \"type\": \"primary\",\n" +
            "                       \"url\": \"%s\""+
            "                    }\n" +
            "                ],\n" +
            "                \"tag\": \"action\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"tag\": \"hr\"\n" +
            "            },\n" +
            "            {\n" +
            "                \"elements\": [\n" +
            "                    {\n" +
            "                        \"content\": \"[%s](%s)\",\n" +
            "                        \"tag\": \"lark_md\"\n" +
            "                    }\n" +
            "                ],\n" +
            "                \"tag\": \"note\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

    public static String getWebHookMessage(AlarmMessage message) {
        return String.format(checkdataFail,
                message.getDegree(),
                message.getTitle(),
                message.getOccurrenceTime(),
                message.getOccurrencePlace(),
                message.getDescribe(),
                message.getConfirmButton(),
                message.getConfirmButtonUrl(),
                message.getMessageSource(),
                message.getUrl());
    }

}

