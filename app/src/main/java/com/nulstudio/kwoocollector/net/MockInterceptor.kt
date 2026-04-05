package com.nulstudio.kwoocollector.net

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val keyword = request.url.queryParameter("keyword").orEmpty()

        Thread.sleep(80)

        val defaultSuccessJson = """{"code":0,"message":null,"result":null}"""

        val responseJson = when {
            path.contains("/verify") -> defaultSuccessJson
            path.contains("/logout") -> defaultSuccessJson
            path.contains("/profile/password") && method == "POST" -> defaultSuccessJson
            path.contains("/login") || path.contains("/register") -> {
                """{"code":0,"message":null,"result":"mock_jwt_token_888999"}"""
            }

            path.contains("/profile") -> {
                """{"code":0,"message":null,"result":{"username":"nullsty","role":"系统管理员"}}"""
            }

            path.contains("/update") -> {
                """{"code":0,"message":null,"result":{"build":999,"version":"v9.9.9-mock","url":"https://example.com/mock_update.apk"}}"""
            }

            path.endsWith("/forms/history") && method == "GET" -> {
                """
                {"code":0,"message":null,"result":[
                  {"id":11,"name":"三月设备巡检记录","deliverTime":1679500000000,"deadline":1679586400000,"priority":2},
                  {"id":12,"name":"二月生产日报","deliverTime":1676800000000,"deadline":1676886400000,"priority":1}
                ]}
                """.trimIndent()
            }

            path.matches(Regex(".*/forms/history/11")) && method == "GET" -> {
                """
                {"code":0,"message":null,"result":{
                  "formId":11,
                  "title":"三月设备巡检记录",
                  "description":"已提交的设备巡检历史记录。",
                  "submittedAt":1679500000000,
                  "fields":[
                    {"type":"text","key":"device_name","label":"设备名称","required":true,"maxLength":50},
                    {"type":"number","key":"temperature","label":"运行温度(℃)","required":true,"min":-20.0,"max":150.0},
                    {"type":"select","key":"status","label":"运行状态","required":true,"options":[{"id":1,"name":"正常运行"},{"id":2,"name":"带病运行"},{"id":3,"name":"已停机"}]},
                    {"type":"bool","key":"need_repair","label":"是否需要报修","required":false},
                    {"type":"image","key":"scene_photos","label":"现场照片","required":false}
                  ],
                  "content":{
                    "device_name":"一号高压泵",
                    "temperature":72.5,
                    "status":1,
                    "need_repair":false,
                    "scene_photos":["https://dummyimage.com/600x400/0a7/fff&text=History+Pump"]
                  }
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/forms/history/\\d+")) && method == "GET" -> {
                """
                {"code":0,"message":null,"result":{
                  "formId":12,
                  "title":"二月生产日报",
                  "description":"已提交的生产日报历史记录。",
                  "submittedAt":1676800000000,
                  "fields":[
                    {"type":"text","key":"workshop","label":"车间名称","required":true,"maxLength":20},
                    {"type":"number","key":"output","label":"日产量(吨)","required":true,"min":0.0,"max":9999.0},
                    {"type":"select","key":"shift","label":"班次","required":true,"options":[{"id":1,"name":"白班"},{"id":2,"name":"夜班"}]},
                    {"type":"bool","key":"abnormal","label":"是否异常","required":false},
                    {"type":"image","key":"attachments","label":"现场照片","required":false}
                  ],
                  "content":{
                    "workshop":"二号车间",
                    "output":1680.0,
                    "shift":2,
                    "abnormal":false,
                    "attachments":["https://dummyimage.com/600x400/222/fff&text=History+Report"]
                  }
                }}
                """.trimIndent()
            }

            path.endsWith("/forms") && method == "GET" -> {
                """
                {"code":0,"message":null,"result":[
                  {"id":1,"name":"每日生产数据上报","deliverTime":1680000000000,"deadline":1680086400000,"priority":3},
                  {"id":2,"name":"设备巡检记录(A区)","deliverTime":1680000000000,"deadline":1680086400000,"priority":2}
                ]}
                """.trimIndent()
            }

            path.matches(Regex(".*/forms/1")) && method == "GET" -> {
                """
                {"code":0,"message":null,"result":{
                  "formId":1,
                  "title":"每日生产数据上报",
                  "description":"请在下班前完成生产数据采集并提交。",
                  "fields":[
                    {"type":"text","key":"workshop","label":"车间名称","required":true,"maxLength":20},
                    {"type":"number","key":"output","label":"日产量(吨)","required":true,"min":0.0,"max":9999.0},
                    {"type":"select","key":"shift","label":"班次","required":true,"options":[{"id":1,"name":"白班"},{"id":2,"name":"夜班"}]},
                    {"type":"bool","key":"abnormal","label":"是否异常","required":false},
                    {"type":"image","key":"attachments","label":"现场照片","required":false}
                  ]
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/forms/\\d+")) && method == "GET" -> {
                """
                {"code":0,"message":null,"result":{
                  "formId":2,
                  "title":"设备巡检记录",
                  "description":"请在现场录入温度、状态并上传照片。",
                  "fields":[
                    {"type":"text","key":"device_name","label":"设备名称","required":true,"maxLength":50},
                    {"type":"number","key":"temperature","label":"运行温度(℃)","required":true,"min":-20.0,"max":150.0},
                    {"type":"select","key":"status","label":"运行状态","required":true,"options":[{"id":1,"name":"正常运行"},{"id":2,"name":"带病运行"},{"id":3,"name":"已停机"}]},
                    {"type":"bool","key":"need_repair","label":"是否需要报修","required":false},
                    {"type":"image","key":"scene_photos","label":"现场照片","required":true}
                  ]
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/forms/\\d+")) && method == "POST" -> defaultSuccessJson

            path.contains("/media/upload") -> {
                """{"code":0,"message":null,"result":"https://dummyimage.com/600x400/000/fff&text=Mock+Image"}"""
            }

            path.endsWith("/tables") && method == "GET" -> {
                """
                {"code":0,"message":null,"result":[
                  {
                    "id":101,
                    "name":"巡检台账表",
                    "schema":[
                      {"type":"text","key":"device_name","label":"设备名称","required":true,"maxLength":50},
                      {"type":"number","key":"temperature","label":"运行温度(℃)","required":true,"min":-20.0,"max":150.0},
                      {"type":"select","key":"status","label":"运行状态","required":true,"options":[{"id":1,"name":"正常运行"},{"id":2,"name":"带病运行"},{"id":3,"name":"已停机"}]},
                      {"type":"bool","key":"need_repair","label":"是否需要报修","required":false},
                      {"type":"image","key":"scene_photos","label":"现场照片","required":false}
                    ]
                  },
                  {
                    "id":102,
                    "name":"产线日报表",
                    "schema":[
                      {"type":"text","key":"line_name","label":"产线名称","required":true,"maxLength":30},
                      {"type":"number","key":"daily_output","label":"当日产量","required":true,"min":0.0,"max":100000.0},
                      {"type":"select","key":"quality","label":"质量评级","required":true,"options":[{"id":1,"name":"A"},{"id":2,"name":"B"},{"id":3,"name":"C"}]},
                      {"type":"bool","key":"maintenance_done","label":"是否完成保养","required":false},
                      {"type":"image","key":"report_images","label":"报告附件","required":false}
                    ]
                  }
                ]}
                """.trimIndent()
            }

            path.matches(Regex(".*/tables/101")) && method == "GET" -> {
                val abstracts = listOf(
                    """{"id":1001,"primary":"1号高压泵","secondary":["正常运行","张三"]}""",
                    """{"id":1002,"primary":"2号冷却机","secondary":["带病运行","李四"]}"""
                ).filter { keyword.isBlank() || it.contains(keyword, ignoreCase = true) }
                """
                {"code":0,"message":null,"result":{
                  "count":${abstracts.size},
                  "abstracts":[${abstracts.joinToString(",")}]
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/tables/102")) && method == "GET" -> {
                val abstracts = listOf(
                    """{"id":2001,"primary":"一号产线","secondary":["A级","1000"]}"""
                ).filter { keyword.isBlank() || it.contains(keyword, ignoreCase = true) }
                """
                {"code":0,"message":null,"result":{
                  "count":${abstracts.size},
                  "abstracts":[${abstracts.joinToString(",")}]
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/tables/101/1001")) && method == "GET" -> {
                """
                {"code":0,"message":null,"result":{
                  "device_name":"1号高压泵",
                  "temperature":68.5,
                  "status":1,
                  "need_repair":false,
                  "scene_photos":["https://dummyimage.com/600x400/0a7/fff&text=Pump"]
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/tables/101/\\d+")) && method == "GET" -> {
                """
                {"code":0,"message":null,"result":{
                  "device_name":"2号冷却机",
                  "temperature":83.0,
                  "status":2,
                  "need_repair":true,
                  "scene_photos":["https://dummyimage.com/600x400/b33/fff&text=Cooler"]
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/tables/102/\\d+")) && method == "GET" -> {
                """
                {"code":0,"message":null,"result":{
                  "line_name":"一号产线",
                  "daily_output":1000.0,
                  "quality":1,
                  "maintenance_done":true,
                  "report_images":["https://dummyimage.com/600x400/222/fff&text=Report"]
                }}
                """.trimIndent()
            }

            path.matches(Regex(".*/tables/\\d+.*")) && (method == "POST" || method == "PUT" || method == "DELETE") -> {
                defaultSuccessJson
            }

            else -> {
                """{"code":404,"message":"Mock 未命中此接口: [$method] $path","result":null}"""
            }
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_2)
            .code(200)
            .message("OK")
            .body(responseJson.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
