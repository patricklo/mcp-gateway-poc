curl -X POST http://localhost:8701/api/v1/mcp/get_company_employee \
  -H "Content-Type: application/json" \
  -d '{
    "city": "beijing",
    "company": {
      "name": "alibaba",
      "type": "internet"
    },
    "employeeCount": "张三"
  }'