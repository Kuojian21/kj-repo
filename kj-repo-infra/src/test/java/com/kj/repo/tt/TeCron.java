package com.kj.repo.tt;

import static com.cronutils.model.CronType.UNIX;

import java.time.Duration;
import java.time.ZonedDateTime;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.FieldExpressionFactory;
import com.cronutils.model.time.ExecutionTime;


public class TeCron {
    public static void main(String[] args) {
        Cron cron = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(UNIX)) //
                .withHour(FieldExpressionFactory.always()) //
                .withMinute(FieldExpressionFactory.every(2)) //
                .instance() //
                .validate();

        ZonedDateTime now = ZonedDateTime.now();
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        Duration n = executionTime.timeToNextExecution(now);
        Duration l = executionTime.timeFromLastExecution(now);
        System.out.println(n.getSeconds());
        System.out.println(l.getSeconds());
        System.out.println(executionTime.timeFromLastExecution(now));
        System.out.println(cron.asString());
    }
}
