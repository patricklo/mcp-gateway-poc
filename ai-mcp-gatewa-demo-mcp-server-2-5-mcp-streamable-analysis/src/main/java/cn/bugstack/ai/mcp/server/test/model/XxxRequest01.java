package cn.bugstack.ai.mcp.server.test.model;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "XxxRequest01", description = "公司员工信息查询请求")
public class XxxRequest01 {

    @JsonProperty(required = true, value = "city")
    @JsonPropertyDescription("城市名称,如果是中文汉字请先转换为汉语拼音,例如北京:beijing")
    @Schema(description = "城市名称(拼音),例如: beijing", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @JsonProperty(required = true, value = "company")
    @JsonPropertyDescription("公司信息,如果是中文汉字请先转换为汉语拼音,例如北京:jd/alibaba")
    @Schema(description = "公司信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private Company company;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(name = "Company", description = "公司信息")
    public static class Company {

        @JsonProperty(required = true, value = "name")
        @JsonPropertyDescription("公司名称")
        @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String name;

        @JsonProperty(required = true, value = "type")
        @JsonPropertyDescription("公司类型")
        @Schema(description = "公司类型", requiredMode = Schema.RequiredMode.REQUIRED)
        private String type;

        @JsonProperty(required = true, value = "deep")
        @JsonPropertyDescription("测试")
        @Schema(description = "测试", requiredMode = Schema.RequiredMode.REQUIRED)
        private Deep deep;

        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(name = "Deep", description = "测试")
        public static class Deep {

            @JsonProperty(required = true, value = "x01")
            @JsonPropertyDescription("测试")
            @Schema(description = "测试", requiredMode = Schema.RequiredMode.REQUIRED)
            private String x01;
        }

    }

}
