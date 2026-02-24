package api.http.routes;

import io.javalin.http.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.*;

public abstract class AbstractHTTPRoute implements Handler {

    private static final Logger log = LoggerFactory.getLogger(AbstractHTTPRoute.class);

    @Override
    public void handle(@NotNull Context context){
        try{
            context.status(200);

            var result = execute(context);

            if(result != null) context.result(result);
        }catch(HttpResponseException e){
            handleException(context, e);
        }catch(Exception e){
            log.error("Exception in HTTP route", e);

            handleError(context, e);
        }
    }

    protected void handleException(Context context, HttpResponseException e){
        context.status(e.getStatus()).result(e.getMessage());
    }

    protected void handleError(Context context, Exception e){
        context.status(500).result("An internal error occurred.");

        log.error("Error in {}", context.path(), e);
    }

    public abstract String execute(Context context);
}
