package cn.bugstack.ai.mcp.server.test.model;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "XxxResponse", description = "公司员工信息查询响应")
public class XxxResponse {

    @JsonProperty(required = true, value = "salary")
    @JsonPropertyDescription("工资")
    @Schema(description = "工资", requiredMode = Schema.RequiredMode.REQUIRED)
    private String salary;

    @JsonProperty(required = true, value = "dayManHour")
    @JsonPropertyDescription("工时")
    @Schema(description = "工时", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dayManHour;

    @JsonProperty(required = true, value = "user")
    @JsonPropertyDescription("用户")
    @Schema(description = "用户信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private User user;

    @Data
    @Schema(name = "User", description = "用户信息")
    public static class User {
        @Schema(description = "用户ID")
        private String userId;
        @Schema(description = "用户名")
        private String userName;
    }

    @Override
    public String toString() {
        return "XxxResponse{" +
                "salary='" + salary + '\'' +
                ", dayManHour='" + dayManHour + '\'' +
                '}';
    }
}
