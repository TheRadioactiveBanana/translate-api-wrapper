package api.http.routes.impl;

import api.http.routes.AbstractHTTPRoute;
import api.language.Language;
import com.alibaba.fastjson.*;
import io.javalin.http.Context;

public class GetLanguagesRoute extends AbstractHTTPRoute {
    @Override
    public String execute(Context context){
        JSONArray array = new JSONArray();
        for(Language language : Language.values()){
            array.add(new JSONObject()
                .fluentPut("code", language.code)
                .fluentPut("name", language.name));
        }
        return array.toString();
    }
}
