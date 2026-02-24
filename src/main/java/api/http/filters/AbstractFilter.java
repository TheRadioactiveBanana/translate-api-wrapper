package api.http.filters;

import io.javalin.http.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.*;


public abstract class AbstractFilter implements Handler {

    private static final Logger log = LoggerFactory.getLogger(AbstractFilter.class);

    public void handle(@NotNull Context context){
        try{
            filter(context);
        }catch(HttpResponseException e){
            context.status(e.getStatus()).result(e.getMessage());
        }catch(Exception e){
            log.error("Error in {}", context.path(), e);
            context.status(500).result("An internal error occurred.");
        }
    }

    public abstract void filter(Context context);
}
