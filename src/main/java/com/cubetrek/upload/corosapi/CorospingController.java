package com.cubetrek.upload.corosapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class CorospingController {
    Logger logger = LoggerFactory.getLogger(CorospingController.class);

    @Value("${coros.client.id}") //aka application_id
    String corosClientId;
    @Value("${coros.client.secret}") //aka accesstoken
    String corosClientSecret;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    //Ping from Coros; this is the webhook
    //Workout Summary Data Push, Chapter 5.3
    @PostMapping(value = "/corosconnect")
    public ResponseEntity corosPing(
                                    @RequestBody String payload, // To capture JSON body parameters
                                    @RequestHeader("client") String client,
                                    @RequestHeader("secret") String secret) { // To capture headers

        logger.info("Coros Ping received: "+payload);
        logger.info("Cient: "+client);
        logger.info("Secret: "+secret);



        //see chapter 5.3.4
        return ResponseEntity.ok("""
                { "message":"ok", "result":"0000" }
                """);
    }

    //Service Status Check, Chapter 5.3
    @GetMapping(value = "/corosconnect/status")
    public ResponseEntity corosStatus() {
        logger.info("Coros: Status Request received");
        return ResponseEntity.ok().build();
    }
}
