package cn.bugstack.ai.mcp.server.test.trigger;

import cn.bugstack.ai.mcp.server.test.model.XxxRequest01;
import cn.bugstack.ai.mcp.server.test.model.XxxResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 测试服务控制器
 * 提供公司员工信息查询接口
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/mcp/")
@Tag(name = "测试服务", description = "公司员工信息查询接口")
public class TestServiceController {

    /**
     * 获取公司员工信息
     *
     * <p>注意:Spring MVC 不支持在一个方法中使用多个 @RequestBody 参数。
     * 当前实现存在问题,需要将两个请求参数合并为一个请求对象。</p>
     *
     * <p><b>curl 请求示例:</b></p>
     * <pre>
     * curl -X POST http://localhost:8701/api/v1/mcp/get_company_employee \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "city": "beijing",
     *     "company": {
     *       "name": "alibaba",
     *       "type": "internet"
     *     }
     *   }'
     * </pre>
     *
     * <p><b>请求参数说明:</b></p>
     * <ul>
     *   <li>city: 城市名称(拼音格式),例如: beijing</li>
     *   <li>company.name: 公司名称,例如: jd, alibaba</li>
     *   <li>company.type: 公司类型,例如: internet, finance</li>
     *   <li>employeeCount: 雇员姓名</li>
     * </ul>
     *
     * <p><b>响应示例:</b></p>
     * <pre>
     * {
     *   "obsTime": "8000",
     *   "obsTime": "8"
     * }
     * </pre>
     *
     * @param xxxRequest01 请求参数1 - 包含城市和公司信息
     * @return XxxResponse 返回员工的工资和工时信息
     */
    @PostMapping("get_company_employee")
    @Operation(
            summary = "获取公司员工信息",
            description = "根据城市与公司信息返回员工工资与工时"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(schema = @Schema(implementation = XxxResponse.class)))
    })
    public XxxResponse getCompanyEmployee(@RequestBody XxxRequest01 xxxRequest01) {
        log.info("查询公司员工信息 - 城市:{}, 公司:{}",
                xxxRequest01.getCity(),
                xxxRequest01.getCompany().getName());

        XxxResponse xxxResponse = new XxxResponse();
        xxxResponse.setSalary("16.92");
        xxxResponse.setDayManHour(String.valueOf(new Random().nextInt(24)));

        XxxResponse.User user = new XxxResponse.User();
        user.setUserId("111");
        user.setUserName("小傅哥");

        xxxResponse.setUser(user);

        XxxResponse xxxResponse02 = new XxxResponse();
        xxxResponse02.setSalary("16.92");
        xxxResponse02.setDayManHour(String.valueOf(new Random().nextInt(24)));

        XxxResponse.User user02 = new XxxResponse.User();
        user02.setUserId("111");
        user02.setUserName("小傅哥");

        xxxResponse02.setUser(user);

        List<XxxResponse> xlist = new ArrayList<>();
        xlist.add(xxxResponse);
        xlist.add(xxxResponse02);

        return xxxResponse;
    }

    @GetMapping("/query-by-id-01")
    public String queryAiClientById01(@RequestParam("id") String id) {
        return "hi xiaofuge " + id;
    }

    @GetMapping("/query-by-id-02")
    public String queryAiClientById02(@RequestParam("id") int id) {
        return "hi xiaofuge " + id;
    }

    @GetMapping("/query-by-id-03")
    public String queryAiClientById03(@RequestParam("id") Integer id) {
        return "hi xiaofuge " + id;
    }

    @GetMapping("/query-test02")
    public String test02(@RequestBody XxxRequest01.Company.Deep deep) {
        return "hi xiaofuge";
    }

    @GetMapping("/query-test03")
    public String test03(@RequestBody XxxRequest01.Company company) {
        return "hi xiaofuge";
    }

}
