package api.http.filters.impl;

import api.TranslateAPIMain;
import api.http.filters.AbstractFilter;
import io.javalin.http.*;

public class AuthFilter extends AbstractFilter {


    @Override
    public void filter(Context context){
        var token = context.header("token");

        if(token == null) throw new BadRequestResponse("Missing token");

        if(!token.equals(TranslateAPIMain.token)) throw new ForbiddenResponse("Invalid token");
        //pass
    }
}
