package ru.open.monitor.statistics.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;

import ru.open.monitor.statistics.event.StatisticsCollector;

public class StatisticsCollectorAspect {

    @Autowired
    private StatisticsCollector statisticsCollector;

    public Object eventProcessed(final ProceedingJoinPoint joinPoint) throws Throwable {
        String handlerClass = joinPoint.getTarget().getClass().getName();

        Object event = joinPoint.getArgs()[0];
        long startNanos = System.nanoTime();
        Object result = joinPoint.proceed();
        long stopNanos = System.nanoTime();

        if (event != null) {
            statisticsCollector.eventProcessed(event.getClass().getName(), handlerClass, stopNanos - startNanos);
        }

        return result;
    }

    public void eventPublished(final JoinPoint joinPoint) {
        String publisherClass = joinPoint.getTarget().getClass().getName();

        Object event = joinPoint.getArgs()[0];

        if (event != null) {
            statisticsCollector.eventPublished(event.getClass().getName(), publisherClass);
        }
    }

    public Object requestPublished(final ProceedingJoinPoint joinPoint) throws Throwable {
        String requestPublisherClass = joinPoint.getTarget().getClass().getName();

        Object request = joinPoint.getArgs()[0];
        long startNanos = System.nanoTime();
        Object reply = joinPoint.proceed();
        long stopNanos = System.nanoTime();

        if (request != null) {
            statisticsCollector.eventPublished(request.getClass().getName(), requestPublisherClass);
        }
        if (reply != null) {
            statisticsCollector.eventProcessed(reply.getClass().getName(), requestPublisherClass, stopNanos - startNanos);
        }

        return reply;
    }

    public Object requestProcessed(final ProceedingJoinPoint joinPoint) throws Throwable {
        String handlerClass = joinPoint.getTarget().getClass().getName();

        Object request = joinPoint.getArgs()[0];
        long startNanos = System.nanoTime();
        Object reply = joinPoint.proceed();
        long stopNanos = System.nanoTime();

        if (reply != null) {
            statisticsCollector.eventPublished(reply.getClass().getName(), handlerClass);
        }
        if (request != null) {
            statisticsCollector.eventProcessed(request.getClass().getName(), handlerClass, stopNanos - startNanos);
        }

        return reply;
    }

}
