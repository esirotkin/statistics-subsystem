Данная подсистема предназначена для мониторинга количества обработанных и опубликованных событий приложением, к которому данная подсистема мониторинга (сбора статистики) подключена.

### <a name="MonitoringDescription" /> Описание подсистемы мониторинга

Подсистема мониторинга состоит из 6 модулей:

* [Statistics Holder](#StatisticsHolderDescription) -- модуль, аккумулирующий собираемую статистику;
* [Statistics Collector AOP](#StatisticsCollectorAOPDescription) -- модуль, собирающий статистику средствами [AOP](http://habrahabr.ru/post/114649/);
* [Statistics Collector JDBC](#StatisticsCollectorJDBCDescription) -- модуль, собирающий статистику о выполнении запросов к БД и обработке полученных результатов в качестве обёртки над [JDBC](http://ru.wikipedia.org/wiki/Java_Database_Connectivity);
* [Statistics Exposer JMX](#StatisticsExposerJMXDescription) -- модуль, экспортирующий статистику в [JMX](http://en.wikipedia.org/wiki/Java_Management_Extensions)-бины;
* [Statistics Exposer Zabbix](#StatisticsExposerZabbixDescription) -- модуль, отправляющий статистику на [Zabbix](http://ru.wikipedia.org/wiki/Zabbix)-сервер средствами [ZabbixSender](http://www.zabbix.com/documentation/3.0/ru/manual/concepts/sender);
* [Statistics Exposer Log](#StatisticsExposerLogDescription) -- модуль, производящий вывод собранной статистики в лог.

Для подключения данного мониторинга к приложению, мониторинг которого необходимо осуществлять, необходимы, как минимум, 3 модуля:

* [Statistics Holder](#StatisticsHolderDescription), в котором будет аккумулироваться собираемая статистика;
* [Statistics Collector AOP](#StatisticsCollectorAOPDescription) для сборки статистики;
* [Statistics Exposer JMX](#StatisticsExposerJMXDescription) и [Statistics Exposer Zabbix](#StatisticsExposerZabbixDescription) для экспорта статистика в [JMX](http://ru.wikipedia.org/wiki/Java_Management_Extensions) и публикации статистики в [Zabbix](http://ru.wikipedia.org/wiki/Zabbix).

### <a name="StatisticsHolderDescription" /> Statistics Holder

Модуль [Statistics Holder](#StatisticsHolderDescription) является агрегатором статистики, поступающей в него из `StatisticsCollector`'а.

Данный модуль аккумулирует:

* количество обработанных приложением событий, а так же минимальное, среднее, максимальное и последнее время обработки каждого из типов событий в *наносекундах*;
* количество опубликованных приложением событий, агрегированное по типам событий.

Данный модуль аггрегирует как *полную* статистику (от момента последнего обнуления статистики), так и статистику за промежуток времени *тайм-фрейм*, определяемый свойством `statistics.monitor.timeFrame`.

*Полная* статистика может быть получена потребителем статистики в любой момент времени, в отличии от статистики за *тайм-фрейм*, которая предоставляется потребителю статистики, реализующему интерфейс [FramedEventStatisticsConsumer](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/frame/FramedEventStatisticsConsumer.java) строго на границе окончания временного промежутка, определённого свойством `statistics.monitor.timeFrame`.

#### <a name="StatisticsHolderConnection" /> Подключение к приложению

Для подключения агрегатора статистики к вашему приложению необходимо в зависимости проекта добавить зависимость от данного модуля, подгрузить его Spring-контекст и определить свойство `statistics.monitor.timeFrame`.

```xml
    <dependency>
        <groupId>io.github.esirotkin</groupId>
        <artifactId>statistics-holder</artifactId>
        <version>1.1.6</version>
    </dependency>
```

```xml
    <import resource="classpath:/META-INF/spring/monitor-context.xml" />
```

Конфигурационные свойства:

* `statistics.monitor.timeFrame` -- свойство, определяющее временной промежуток *тайм-фрейм* в *миллисекундах* (параметр *необязательный*: значение по умолчанию = 15000 *миллисекунд*);

#### <a name="StatisticsHolderInterfaces" /> Интерфейсы Statistics Holder'а

##### Сбор статистики об обработанных и опубликованных приложением событиях

Для взаимодействия с агрегатором статистики предоставляются 9 интерфейсов:

* [StatisticsCollector](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/StatisticsCollector.java) -- для сбора статистики об обработанных и опубликованных событиях;
* [StatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/StatisticsProvider.java) -- для получения *полной* статистики об обработанных и опубликованных событиях;
* [FramedEventStatisticsSubscription](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/frame/FramedEventStatisticsSubscription.java) -- для подписки на получение статистики за временной промежуток *тайм-фрейм*;
* [FramedEventStatisticsConsumer](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/frame/FramedEventStatisticsConsumer.java) -- должен быть реализован потребителем статистики за временной промежуток *тайм-фрейм*;
* [FramedEventStatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/frame/FramedEventStatisticsProvider.java) -- интерфейс, наследуемый от [StatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/StatisticsProvider.java), предоставляемый потребителю статистики за временной промежуток *тайм-фрейм* для получения этой самой статистики;
* [QueueStatisticsCollector](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/queue/QueueStatisticsCollector.java) -- для сбора статистики работы очереди обработки событий вашего приложения/сервиса (при наличии таковой);
* [QueueStatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/queue/QueueStatisticsProvider.java) -- для получения статистики работы очереди обработки событий вашего приложения/сервиса (при наличии таковой);
* [QueueStatisticsCleaner](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/queue/QueueStatisticsCleaner.java) -- для очистки статистики работы очереди обработки событий вашего приложения/сервиса (при наличии таковой);
* [StatisticsCleaner](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/StatisticsCleaner.java) -- для очистки всей собранной статистики.

С интерфейсом [StatisticsCollector](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/StatisticsCollector.java) взаимодействуют сборщики статистики, такие как [Statistics Collector AOP](#StatisticsCollectorAOPDescription).

С интерфейсом [StatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/StatisticsProvider.java) взаимодействуют экспортёры статистики, такие как [Statistics Exposer JMX](#StatisticsExposerJMXDescription) и [Statistics Exposer Log](#StatisticsExposerLogDescription).

С интерфейсами [FramedEventStatisticsSubscription](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/frame/FramedEventStatisticsSubscription.java), [FramedEventStatisticsConsumer](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/frame/FramedEventStatisticsConsumer.java) и [FramedEventStatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/frame/FramedEventStatisticsProvider.java) так же взаимодействуют экспортёры статистики, такие как [Statistics Exposer Zabbix](#StatisticsExposerZabbixDescription) и [Statistics Exposer Log](#StatisticsExposerLogDescription).

С этими интерфейсами вам взаимодействовать внутри вашего приложения в явном виде вряд ли придётся.

##### Сбор статистики о работе очереди обработки событий

Для сбора статистики о работе очереди обработки событий необходимо в класс, реализующий очередь обработки событий, заинжектить бин, реализующий интерфейс [QueueStatisticsCollector](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/queue/QueueStatisticsCollector.java).

```java
    @Autowired
    private QueueStatisticsCollector statisticsCollector;
```

И при каждом добавлении нового события в очередь вызывать метод `statisticsCollector.eventQueued()`, а после каждого обработанного события вызывать метод `statisticsCollector.eventProcessed()`.

##### Сбор статистики о выполнении запросов к БД и обработке полученных результатов

Для мониторинга статистики взаимодействия с БД предназначен отдельный `StatisticsHolder`, кототрый предоставляет 6 интерфейсов, аналогичных вышеописанным интерфейсам:

* [StatisticsCollector](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/database/StatisticsCollector.java) -- для сбора статистики о выполненных запросах и обработанных записях (в каждом из `ResultSet`ов);
* [StatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/database/StatisticsProvider.java) -- для получения *полной* статистики о выполненных запросах и обработанных записях;
* [FramedDbStatisticsSubscription](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/database/frame/FramedDbStatisticsSubscription.java) -- для подписки на получение статистики за временной промежуток *тайм-фрейм*;
* [FramedDbStatisticsConsumer](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/database/frame/FramedDbStatisticsConsumer.java) -- должен быть реализован потребителем статистики за временной промежуток *тайм-фрейм*;
* [FramedDbStatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/database/frame/FramedDbStatisticsProvider.java) -- интерфейс, наследуемый от [StatisticsProvider](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/database/StatisticsProvider.java), предоставляемый потребителю статистики за временной промежуток *тайм-фрейм* для получения этой самой статистики;
* [StatisticsCleaner](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/database/StatisticsCleaner.java) -- для очистки всей собранной статистики.

Как и в случае с интерфейсами, ответственными за сбор и получение статистики об обработке и публикации событий (`Event`), взаимодействовать напрямую с данными интерфейсами нет необходимости.
Для сбора статистики об исполнении запросов БД и обработке полученных в результате исполнения запроса результатов предназначен модуль [Statistics Collector JDBC](#StatisticsCollectorJDBCDescription), а публикацией собранной статистики в Zabbix занимается общий модуль [Statistics Exposer Zabbix](#StatisticsExposerZabbixDescription).

##### Обнуление статистики

Для обнуления собранной за определённый период статистики (например, за сутки; или по факту получения события `CleanEvent`) следует воспользоваться интерфейсом [StatisticsCleaner](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-holder/src/main/java/ru/open/monitor/statistics/event/StatisticsCleaner.java), предваритеьлно заинжектив соответствующий бин в своё приложение/сервис.

```java
    @Autowired
    private StatisticsCleaner statisticsCleaner;
```

Далее обнулять собранную статистику в любой момент времени следует вызовом метода `statisticsCleaner.clear()`.


### <a name="StatisticsCollectorAOPDescription" /> Statistics Collector AOP

Модуль [Statistics Collector AOP](#StatisticsCollectorAOPDescription) предназначен для сбора статистики средствами [Spring AOP](http://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#aop).

Использование подхода [AOP](http://ru.wikipedia.org/wiki/%D0%90%D1%81%D0%BF%D0%B5%D0%BA%D1%82%D0%BD%D0%BE-%D0%BE%D1%80%D0%B8%D0%B5%D0%BD%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%BD%D0%BE%D0%B5_%D0%BF%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%BC%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5) оправдано тогда, когда сложно внедрить в ваше приложение дополнительные [Proxy](http://ru.wikipedia.org/wiki/Proxy_%28%D1%88%D0%B0%D0%B1%D0%BB%D0%BE%D0%BD_%D0%BF%D1%80%D0%BE%D0%B5%D0%BA%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F%29)-объекты для организации сбора статистики.

#### <a name="StatisticsCollectorAOPConnection" /> Подключение к приложению

Для подключения данного сборщика статистики к вашему приложению необходимо в зависимости проекта добавить зависимость от данного модуля и подгрузить Spring-контекст, в котором вы должны будете определить [конфигурацию AOP](http://docs.spring.io/spring/docs/current/spring-framework-reference/core.html#aop-schema) аналогично примеру, приведённому ниже.

```xml
    <dependency>
        <groupId>io.github.esirotkin</groupId>
        <artifactId>statistics-collector-aop</artifactId>
        <version>1.1.6</version>
    </dependency>
```

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd
                           http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <import resource="classpath:/META-INF/spring/monitor-aop-context.xml" />

    <aop:aspectj-autoproxy proxy-target-class="true" />

    <aop:config>
        <aop:aspect id="processEventAspect" ref="statisticsCollectorAspect">
            <aop:pointcut id="processEvent" expression="execution(public !static void ru.open..*.onEvent*(..))" />
            <aop:around pointcut-ref="processEvent" method="eventProcessed" />
        </aop:aspect>
        <aop:aspect id="publishEventAspect" ref="statisticsCollectorAspect">
            <aop:pointcut id="publishEvent" expression="execution(public !static void ru.open..*.consumeEvent(..))" />
            <aop:after pointcut-ref="publishEvent" method="eventPublished" />
        </aop:aspect>
        <aop:aspect id="sendRequestAspect" ref="statisticsCollectorAspect">
            <aop:pointcut id="sendRequest" expression="execution(public !static * ru.open..*.sendRequest(..))" />
            <aop:around pointcut-ref="sendRequest" method="requestPublished" />
        </aop:aspect>
        <aop:aspect id="processRequestAspect" ref="statisticsCollectorAspect">
            <aop:pointcut id="processRequest" expression="execution(public !static * ru.open..*.processRequest(..))" />
            <aop:around pointcut-ref="processRequest" method="requestProcessed" />
        </aop:aspect>
    </aop:config>

</beans>
```


### <a name="StatisticsCollectorJDBCDescription" /> Statistics Collector JDBC

Модуль [Statistics Collector JDBC](#StatisticsCollectorJDBCDescription) предназначен для сбора статистики о выполнении запросов к БД и обработке результатов, полученных в результате исполнения запросов.

#### <a name="StatisticsCollectorJDBCConnection" /> Подключение к приложению

Для подключения данного сборщика статистики к вашему приложению необходимо, прежде всего, в зависимости проекта добавить зависимость от данного модуля.

```xml
    <dependency>
        <groupId>io.github.esirotkin</groupId>
        <artifactId>statistics-collector-jdbc</artifactId>
        <version>1.1.6</version>
    </dependency>
```

Далее необходимо подгрузить необходимые `Spring`-контексты.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <import resource="classpath:/META-INF/spring/monitor-context.xml" />
    <import resource="classpath:/META-INF/spring/monitor-jdbc-context.xml" />
    <import resource="classpath:/META-INF/spring/monitor-zabbix-context.xml" />

</beans>
```

А в приложении необходимо каждый JDBC `Connection` обернуть в [ConnectionWrapper](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-collector-jdbc/src/main/java/ru/open/monitor/statistics/jdbc/ConnectionWrapper.java) (как показано ниже).

```java
    @Autowired
    private ConnectionWrapperFactory wrapperFactory;

    public Connection createConnection() throws SQLException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Connection connection = ...; // some implementation
        return wrapperFactory.wrapConnection(connection);
    }
```

И дальше использовать полученную обёртку в вашем приложении, как обычный JDBC `Connection`.


### <a name="StatisticsExposerJMXDescription" /> Statistics Exposer JMX

Модуль [Statistics Exposer JMX](#StatisticsExposerJMXDescription) предназначен для экспорта собранной статистики в [JMX](http://en.wikipedia.org/wiki/Java_Management_Extensions)-бины.

В JMX-бины транслируется *полная* статистика, собранная от момента последнего обнуления статистики.

Для взаимодействия с этими JMX-бинами можно воспользоваться инструментом [JConsole](http://docs.oracle.com/javase/8/docs/technotes/guides/management/jconsole.html), поставляемым в комплекте с [JDK](http://ru.wikipedia.org/wiki/Java_Development_Kit).

При инициализации Spring-контекста данного модуля в JMX экспортируются 3 бина:

* `ru.open.monitor.statistics:ProcessedEvents` -- предоставляет информацию об обработанных вашим приложением событиях;
* `ru.open.monitor.statistics:PublishedEvents` -- предоставляет информацию об опубликованных вашим приложением событиях;
* `ru.open.monitor.statistics:StatisticsCleaner` -- предоставляет возможность обнуления статистики вашего приложения посредством вызова метода `clearStatistics()`.

Бины `ProcessedEvents` и `PublishedEvents` статистику об обработанных и опубликованных событиях предоставляют в виде атрибутов, имена которых соответствуют полному имени Java-класса события.

##### ProcessedEvents

Типы обработанных событий (атрибуты JMX-бина), по которым информация будет отображаться в бине `ProcessedEvents`, должны быть указаны в свойстве `statistics.monitor.jmx.interestProcessedEvents` в виде полных имён Java-классов, перечисленных через запятую.

По каждому типу события (в виде составного типа данных) предоставляется следующая информация (в последовательности нижеследующего списка):

* имя класса потребителя событий (в виде префикса аттрибута);
* количество обработанных событий -- суффикс `count`;
* минимальное время обработки события в *секундах* (суффикс `min`);
* среднее время обработки события в *секундах* (суффикс `avg`);
* максимальное время обработки события в *секундах* (суффикс `max`);
* последнее время обработки события в *секундах* (суффикс `last`).

Так же данный бин предоставляет возможность получить список всех типов обрабатываемых данным приложением событий посредством вызова метода `getProcessedEventsClasses()`.

Так же есть возможность получить список *интересующих* событий (тех, что перечислены в свойстве `statistics.monitor.jmx.interestProcessedEvents`) посредством вызова метода `getInterestEventsClasses()`.

##### PublishedEvents

Типы опубликованных событий (атрибуты JMX-бина), по которым информация будет отображаться в бине `PublishedEvents`, должны быть указаны в свойстве `statistics.monitor.jmx.interestPublishedEvents` в виде полных имён Java-классов, перечисленных через запятую.

По каждому типу события (в виде составного типа данных) предоставляется следующая информация:

* имя класса публикатора событий (в виде префикса аттрибута);
* количество опубликованных событий -- суффикс `count`.

Так же данный бин предоставляет возможность получить список всех типов публикуемых данным приложением событий посредством вызова метода `getPublishedEventsClasses()`.

Так же есть возможность получить список *интересующих* событий (тех, что перечислены в свойстве `statistics.monitor.jmx.interestPublishedEvents`) посредством вызова метода `getInterestEventsClasses()`.

#### <a name="StatisticsExposerJMXConnection" /> Подключение к приложению

Для подключения JMX-exposer'а к вашему приложению необходимо в зависимости проекта добавить зависимость от данного модуля и подгрузить его Spring-контекст.

```xml
    <dependency>
        <groupId>io.github.esirotkin</groupId>
        <artifactId>statistics-exposer-jmx</artifactId>
        <version>1.1.6</version>
    </dependency>
```

```xml
    <import resource="classpath:/META-INF/spring/monitor-jmx-context.xml" />
```

#### <a name="StatisticsExposerJMXToZabbix" /> Получение статистики в Zabbix

Для организации мониторинга Java-приложения (средствами JMX) в [Zabbix](http://www.zabbix.com/ru/) наиболее распространены 2 подхода:

* использование нативного [Zabbix Java Gateway](http://www.zabbix.com/documentation/3.0/ru/manual/concepts/java), выступающего в роли Zabbix-прокси;
* использование JMX-HTTP моста [Jolokia](http://jolokia.org/), [предоставляющего доступ к атрибутам JMX-бинов черех HTTP](http://habrahabr.ru/post/137641/).

##### Zabbix Java Gateway

Для [получения в Zabbix значений JMX-атрибутов посредством Java Gateway](http://www.zabbix.com/documentation/3.0/ru/manual/config/items/itemtypes/jmx_monitoring) ключ атрибута будет выглядеть следующим образом:

* `jmx["ru.open.monitor.statistics:name=ProcessedEvents","<eventTypeEscaped>.<handlerSimpleType>\.count"]` -- для получения количества обработанных обработчиком `handlerType` событий типа `eventType`;
* `jmx["ru.open.monitor.statistics:name=ProcessedEvents","<eventTypeEscaped>.<handlerSimpleType>\.min"]` -- для получения минимального времени обработки события типа `eventType` (в *секундах*) обработчиком `handlerType`;
* `jmx["ru.open.monitor.statistics:name=ProcessedEvents","<eventTypeEscaped>.<handlerSimpleType>\.avg"]` -- для получения среднего времени обработки события типа `eventType` (в *секундах*) обработчиком `handlerType`;
* `jmx["ru.open.monitor.statistics:name=ProcessedEvents","<eventTypeEscaped>.<handlerSimpleType>\.max"]` -- для получения максимального времени обработки события типа `eventType` (в *секундах*) обработчиком `handlerType`;
* `jmx["ru.open.monitor.statistics:name=ProcessedEvents","<eventTypeEscaped>.<handlerSimpleType>\.last"]` -- для получения последнего времени обработки события типа `eventType` (в *секундах*) обработчиком `handlerType`;
* `jmx["ru.open.monitor.statistics:name=PublishedEvents","<eventTypeEscaped>.<publisherSimpleType>\.count"]` -- для получения количества опубликованных публикатором `publisherType` событий типа `eventType`.

Суффикс `Escaped` означает, что "точки" `.` в полном имени Java-класса события должны быть экранированы (заменены на `\.`).
Суффикс `Simple` означает, что имя класса обработчика/публикатора представлено в простой форме (без указания пакета).


### <a name="StatisticsExposerZabbixDescription" /> Statistics Exposer Zabbix

Модуль [Statistics Exposer Zabbix](#StatisticsExposerZabbixDescription) предназначен для отправки статистики в Zabbix.

В Zabbix статистика отправляется за *тайм-фрейм* -- промежуток времени, определяемый свойством `statistics.monitor.timeFrame`.

В отличии от вышеописанного варианта получения статистики об обработанных и опубликованных событиях в Zabbix средствами Zabbix Java Gateway через JMX, данный модуль сам отправляет статистику в Zabbix по таймеру (определяемому свойством `statistics.monitor.timeFrame`), а не ожидает, когда его Zabbix опросит.

Следует отметить, что средствами `ZabbixSender` в Zabbix отправляется статистика только о *количестве* обработанных и опубликованных сообщений. Дополнительная статистика о времени обработки сообщения обновляется на сервере Zabbix средствами Zabbix Java Gateway.

#### <a name="StatisticsExposerZabbixConnection" /> Подключение к приложению

Для подключения данного модуля к вашему приложению необходимо в зависимости проекта добавить зависимость от данного модуля, подгрузить его Spring-контекст и определить несколько свойств.

```xml
    <dependency>
        <groupId>io.github.esirotkin</groupId>
        <artifactId>statistics-exposer-zabbix</artifactId>
        <version>1.1.6</version>
    </dependency>
```

```xml
    <import resource="classpath:/META-INF/spring/monitor-zabbix-context.xml" />
```

Конфигурационные свойства:

* `statistics.monitor.zabbix.enable` -- свойство, включающее/отключающее отправку статистики в Zabbix средствами `ZabbixSender`;
* `statistics.monitor.zabbix.server.host` -- DNS-имя или IP-адрес сервера Zabbix;
* `statistics.monitor.zabbix.sender.port` -- номер порта сервера Zabbix для взаимодействия по протоколу ZBX посредством [Zabbix Sender](http://www.zabbix.com/documentation/3.0/ru/manual/concepts/sender) (параметр *необязательный*: значение по умолчанию = 10051);
* `statistics.monitor.zabbix.api.port` -- номер порта сервера Zabbix для взаимодействия по протоколу [Zabbix API](http://www.zabbix.com/documentation/3.0/ru/manual/api) (параметр *необязательный*: значение по умолчанию = 80);
* `statistics.monitor.zabbix.server.user` -- имя пользователя Zabbix, обладающего правами просмотра и создания новых хостов (hosts), интерфейсов (host interfaces), приложений (applications) и элементов (items);
* `statistics.monitor.zabbix.server.password` -- пароль вышеобозначенного пользователя Zabbix;
* `statistics.monitor.zabbix.proxy.list` -- список хостов (и портов) [Zabbix Proxy](http://www.zabbix.com/documentation/3.0/ru/manual/concepts/proxy) в нотации IPv4, перечисляемых через запятую (параметр *необязательный*: в случае отсутствия будет использовано значение `statistics.monitor.zabbix.server.host`:`statistics.monitor.zabbix.sender.port`);
* `statistics.monitor.zabbix.onDemandSendRhythm` -- ритм (периодичность) отправки значений *по требованию* в Zabbix (параметр *необязательный*: значение по умолчанию = 500 мс);
* `statistics.monitor.zabbix.host.group` -- имя группы хостов (host group), к которой принадлежит данный сервер (на котором запущено данное приложение);
* `statistics.monitor.zabbix.simpleNames` -- использовать простые имена для элементов данных (item) в Zabbix (параметр *необязательный*: значение по умолчанию = `true`);
* `statistics.monitor.zabbix.applyTemplates` -- применять ли перечисленные в параметре `statistics.monitor.zabbix.templates` шаблоны к данному хосту (параметр *необязательный*: значение по умолчанию = `false`);
* `statistics.monitor.zabbix.templates` -- имена шаблонов Zabbix (например, `Template JMX Generic`), перечисленные через запятую, которые должны быть применены к данному хосту (параметр *необязательный*);
* `statistics.monitor.zabbix.configureJvmMonitor` -- настраивать ли в Zabbix *элементы данных*, *триггеры* и *графики* для базового мониторинга JVM (параметр *необязательный*: значение по умолчанию = `false`);
* `statistics.monitor.zabbix.api.url` -- URL Zabbix API (параметр *необязательный*: при отсутствии значения URL будет сформирован на основе `statistics.monitor.zabbix.server.host` и `statistics.monitor.zabbix.api.port`);
* `statistics.monitor.zabbix.agent.subnet` -- [IPv4](http://ru.wikipedia.org/wiki/IPv4)-подсеть в нотации [CIDR](http://ru.wikipedia.org/wiki/%D0%91%D0%B5%D1%81%D0%BA%D0%BB%D0%B0%D1%81%D1%81%D0%BE%D0%B2%D0%B0%D1%8F_%D0%B0%D0%B4%D1%80%D0%B5%D1%81%D0%B0%D1%86%D0%B8%D1%8F) для автоматического определения IP-адреса данного `ZabbixSender`'а (параметр *необязательный*: значение по умолчанию = 10.0.0.0/8);
* `statistics.monitor.zabbix.agent.app` -- имя приложения для идентификации отправителя в Zabbix (вместо данного параметра можно определить свойство `spring.application.name`);
* `statistics.monitor.zabbix.agent.host` -- IP адрес или доменное имя хоста, видимого Zabbix серверу (или прокси) для случая запуска приложения в контейнере с внутренней подсетью (параметр *необязательный*: значение по умолчанию = `java.rmi.server.hostname` или автоматически определённый IP адрес локального хоста);
* `statistics.monitor.zabbix.agent.hostname` -- имя хоста, передаваемое Zabbix при конфигурировании хоста (параметр *необязательный*: значение по умолчанию = `hostname`);
* `statistics.monitor.zabbix.interestProcessedEvents` -- типы обрабатываемых событий, перечисленных через запятую, по которым статистика должна отправляться в Zabbix (параметр *необязательный*: по умолчанию статистика в Zabbix будет отправляться по всем обработанным событиям);
* `statistics.monitor.zabbix.interestPublishedEvents` -- типы публикуемых событий, перечисленных через запятую, по которым статистика должна отправляться в Zabbix (параметр *необязательный*: по умолчанию статистика в Zabbix будет отправляться по всем опубликованным событиям);
* `statistics.monitor.zabbix.interestDatabaseStatements` -- имена (идентификаторы) запросов к БД, перечисленных через запятую, по которым статистика должна отправляться в Zabbix (параметр *необязательный*: по умолчанию статистика в Zabbix будет отправляться по всем запросам к базе данных);
* `statistics.monitor.jmx.interestProcessedEvents` -- типы обрабатываемых событий, перечисленных через запятую, для которых в Zabbix будет настроен их мониторинг средствами Zabbix Java Gateway (параметр *необязательный*: при отсутствии таковых JMX-мониторинг для обрабатываемых событий настраиваться НЕ будет);
* `statistics.monitor.jmx.interestPublishedEvents` -- типы публикуемых событий, перечисленных через запятую, для которых в Zabbix будет настроен их мониторинг средствами Zabbix Java Gateway (параметр *необязательный*: при отсутствии таковых JMX-мониторинг для публикуемых событий настраиваться НЕ будет);
* `statistics.monitor.zabbix.includeEventsExposedToJmx` -- конфигурировать Zabbix для получения статистики по событиям, перечисленным только в параметрах `statistics.monitor.jmx.interestProcessedEvents` и `statistics.monitor.jmx.interestPublishedEvents` (параметр *необязательный*: значение по умолчанию = `false`);
* `statistics.monitor.zabbix.jmx.delay` -- периодичность (в *секундах*) обновления значений, отслеживаемых Zabbix'ом средствами Zabbix Java Gateway (параметр *необязательный*: значение по умлочанию = 30 *секунд*);
* `statistics.monitor.zabbix.attemptCount` -- количество попыток, которые будут предприняты для отправки статистики в Zabbix (параметр *необязательный*: значение по умолчанию = 5 попыток);
* `statistics.monitor.zabbix.attemptInterval` -- интервал времени, определяющий периодичность (после первой неудачной) попыток отправки статистики в Zabbix (параметр *необязательный*: значение по умолчанию = 500 *миллисекунд*);
* `statistics.monitor.zabbix.numberOfConfigurationAttempts` -- количество попыток, предпринимаемых для конфигурирования сервера Zabbix (параметр *необязательный*: значение по умолчанию = 5 попыток);
* `java.rmi.server.hostname` -- [IPv4](http://ru.wikipedia.org/wiki/IPv4)-адрес сервера, на которм запущено данное приложение (параметр *необязательный*: при отсутствии значения IP-адрес будет запрошен у операционной системы с фильтрацией по `statistics.monitor.zabbix.agent.subnet`);
* `com.sun.management.jmxremote.port` -- номер порта JMX данного приложения;
* `statistics.monitor.zabbix.deleteObsoleteItems` -- удалять из Zabbix устаревшие элементы данных (параметр *необязательный*: значение по умолчанию = `true`);
* `statistics.monitor.zabbix.numberOfEmptyDaysForObsoleteItem` -- количество дней, по истечении которых элементы данных (items) следует считать неактуальными (при отсутствии "свежих" значений) и удалять (параметр *необязательный*: значение по умолчанию = 90 дней);
* `statistics.monitor.zabbix.protectedItemNames` -- имена (или их части) элементов данных Zabbix (перечисленные через запятую), защищённых от удаления (параметр *необязательный*).

#### Автоматическое конфигурирование Zabbix для отправки статистики на сервер

При включенном свойстве `statistics.monitor.zabbix.enable` в `true` [ZabbixConfigurer](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-exposer-zabbix/src/main/java/ru/open/monitor/statistics/zabbix/config/ZabbixConfigurerImpl.java) перед отправкой статистики в Zabbix средствами `ZabbixSender` производит конфигурирование сервера Zabbix для приёма отправляемой вашим приложением статистики.

Конфигурирование состоит из следующих этапов:

* проверки сконфигурированного хоста с соответствующим JMX-интерфейсом -- при отсутствии таковых они создаются;
* применении шаблонов Zabbix, перечисленных через запятую в свойстве `statistics.monitor.zabbix.templates` к данному хосту;
* проверки сконфигурированных групп элементов (`Application` в терминологии Zabbix): `ProcessedEvents` и `PublishedEvents`, `QueueStatistics`, `ExecutedStatements` и `ProcessedResultSets` -- при отсутствии таковых они создаются;
* проверки сконфигурированных элементов (`Item` в терминологии Zabbix) -- по элементу на каждый статистический показатель -- при отсутствии таковых (или добавлении нового) они создаются.

При этом следует отметить, что средствами `ZabbixSender` (с периодичностью, определённой в свойстве `statistics.monitor.timeFrame`) отправляется статистика лишь о количестве и среднем за *тайм-фрейм* времени обработки обработанных и количестве опубликованных событий, а так же статистика о работе очереди обработки событий (при наличии таковой).
Иные показатели, такие как минимальное, максимальное и последнее зафиксированное время обработки события даже в случае, если данный тип события указан в свойстве `statistics.monitor.zabbix.interestProcessedEvents`, будут обновляться в Zabbix'е средствами Zabbix Java Gateway (JMX) с той периодичностью, кот. определена в свойстве `statistics.monitor.zabbix.jmx.delay`.
Это целесообразно с точки зрения оптимизации трафика.

#### <a name="StatisticsExposerZabbixKeyValue" /> Формат сообщения

[ZabbixSender](http://github.com/hengyunabc/zabbix-sender) отправляет на сервер Zabbix сообщения в [JSON](http://ru.wikipedia.org/wiki/JSON)-формате, где ключ `key` -- это соответствующий атрибут, а `value` -- его значение.

##### ProcessedEvents

Статистика об обработанных сообщениях каждого типа отправляются в Zabbix в виде пары `key`/`value`:

* `host`: `<zabbixApplicationHost>`
    * `zabbixApplicationHost` -- имя приложения из свойства `statistics.monitor.zabbix.agent.app` или `spring.application.name`
* `key`: `ProcessedEvents.<eventSimpleClass>.<handlerSimpleClass>.<itemName>`
    * `eventSimpleClass` -- простое (без префикса пакета) имя Java-класса обрабатываемого события
    * `handlerSimpleClass` -- простое (без префикса пакета) имя Java-класса обработчика события
    * `itemName` -- имя элемента данных (используются: `count` или `avg`)
* `value`: `<eventCount>` или `<avgTime>`
    * `eventCount` -- количество обработанных сообщений данного типа (пересчитанное на 1 секунду)
    * `avgTime` -- среднее за *тайм-фрейм* время в *секундах* обработки события данного типа

##### PublishedEvents

Статистика об опубликованных сообщениях каждого типа отправляются в Zabbix в виде пары `key`/`value`:

* `host`: `<zabbixApplicationHost>`
    * `zabbixApplicationHost` -- имя приложения из свойства `statistics.monitor.zabbix.agent.app` или `spring.application.name`
* `key`: `PublishedEvents.<eventSimpleClass>.<publisherSimpleClass>.<itemName>`
    * `eventSimpleClass` -- простое (без префикса пакета) имя Java-класса публикуемого события
    * `publisherSimpleClass` -- простое (без префикса пакета) имя Java-класса публикатора события
    * `itemName` -- имя элемента данных (используется: `count`)
* `value`: `<eventCount>`
    * `eventCount` -- количество опубликованных сообщений данного типа (пересчитанное на 1 секунду)

##### QueueStatistics

Статистика о работе очереди обработки событий (при наличии таковой) отправляются в Zabbix в виде пары `key`/`value`:

* `host`: `<zabbixApplicationHost>`
    * `zabbixApplicationHost` -- имя приложения из свойства `statistics.monitor.zabbix.agent.app` или `spring.application.name`
* `key`: `QueueStatistics.<parameterName>.<itemName>`
    * `parameterName` -- имя параметра: `QueuedEvent` или `ProcessedEvent`
    * `itemName` -- имя элемента данных (используется: `count`)
* `value`: `<processedEventCount>` или `<queuedEventCount>`
    * `queuedEventCount` -- количество сообщений (необработанных), оставшихся в очереди обработки событий
    * `processedEventCount` -- общее количество обработанных за *тайм-фрейм* сообщений (пересчитанное на 1 секунду)

##### ExecutedStatements

Статистика о выполненных запросах к БД отправляются в Zabbix в виде пары `key`/`value`:

* `host`: `<zabbixApplicationHost>`
    * `zabbixApplicationHost` -- имя приложения из свойства `statistics.monitor.zabbix.agent.app` или `spring.application.name`
* `key`: `ExecutedStatements.<statement>.<itemName>`
    * `statement` -- имя (идентификатор) запроса к БД (без перечисления параметров, пробелы заменены знаком `_`)
    * `itemName` -- имя элемента данных (используются: `count` или `avg`)
* `value`: `<executionCount>` или `<avgTime>`
    * `executionCount` -- количество исполнений запроса / вызова процедуры (пересчитанное на 1 секунду)
    * `avgTime` -- среднее за *тайм-фрейм* время в *секундах* выполнения запроса

##### ProcessedResults

Статистика обработки записей, полученных в результате выполнения запросов к БД, отправляются в Zabbix в виде пары `key`/`value`:

* `host`: `<zabbixApplicationHost>`
    * `zabbixApplicationHost` -- имя приложения из свойства `statistics.monitor.zabbix.agent.app` или `spring.application.name`
* `key`: `ProcessedResultSets.<statement>.<resultSetNumber>.<itemName>`
    * `statement` -- имя (идентификатор) запроса к БД (без перечисления параметров, пробелы заменены знаком `_`)
    * `resultSetNumber` -- порядковый номер `ResultSet`а (нумерация с `0`)
    * `itemName` -- имя элемента данных (используются: `count` или `avg`)
* `value`: `<recordCount>` или `<avgTime>`
    * `recordCount` -- количество обработанных записей БД в рамках данного `ResultSet`а (пересчитанное на 1 секунду)
    * `avgTime` -- среднее за *тайм-фрейм* время в *секундах* обработки каждой записи

#### <a name="StatisticsExposerZabbixOnDemanSender" /> Отправка в Zabbix "неопределённых" данных "по требованию"

Иногда возникает необходимость отправления в Zabbix *неопределённых* (то есть, не фиксированных каким-либо форматом или соглашением) данных *по требованию* (то есть, не по таймеру, а в момент возникновения такой необходимости).

Такая возможность предусмотрена в данном модуле.

Для того, чтобы ей воспользоваться, необходимо в вашем `Spring`-контексте дополнительно инициализировать бин `OnDemandZabbixSender` как показано ниже.

```xml
    <import resource="classpath:/META-INF/spring/monitor-zabbix-context.xml" />

    <bean id="onDemandZabbixSender" class="ru.open.monitor.statistics.zabbix.OnDemandZabbixSender" />
```

И, заинжектив его в своё приложение, отправлять в Zabbix *неопределённые* данные, реализующие интерфейс [ExternalItem](https://github.com/esirotkin/statistics-subsystem/blob/master/statistics-exposer-zabbix/src/main/java/ru/open/monitor/statistics/zabbix/config/item/ExternalItem.java).

```java
    @Autowired
    private OnDemandZabbixSender zabbixSender;

    public void sendToZabbix(SomeExternalItem item) {
        zabbixSender.sendToZabbix(item);
    }
```


### <a name="StatisticsExposerLogDescription" /> Statistics Exposer Log

Модуль [Statistics Exposer Log](#StatisticsExposerLogDescription) предназначен для периодического вывода статистики в лог.

В лог может выводиться как *полная* статистика (от момента последнего обнуления статистики), так и статистика *за промежуток времени* / *тайм-фрейм*, определённый свойством `statistics.monitor.timeFrame`.

#### <a name="StatisticsExposerLogConnection" /> Подключение к приложению

Для подключения данного модуля к вашему приложению необходимо в зависимости проекта добавить зависимость от данного модуля, подгрузить его Spring-контекст, определить несколько свойств и настроить вывод в лог.

```xml
    <dependency>
        <groupId>io.github.esirotkin</groupId>
        <artifactId>statistics-exposer-log</artifactId>
        <version>1.1.6</version>
    </dependency>
```

```xml
    <import resource="classpath:/META-INF/spring/monitor-log-context.xml" />
```

Для логирования *полной* статистики необходимо настроить логирование класса `ru.open.monitor.statistics.log.FullStatisticsLogger`, уровень логирования указать в соответствии со свойством `statistics.monitor.log.fullLoggerLevel`.

```xml
    <property name="LOG_PATH" value="${LOG_PATH:-./log}" />
    <logger name="ru.open.monitor.statistics.log.FullStatisticsLogger" level="TRACE" additivity="false">
        <appender class="ru.open.monitor.statistics.log.logback.layout.appender.StateFileAppender">
            <file>${LOG_PATH}/statistics/statistics-full.log</file>
            <encoder>
                <pattern>%d{dd.MM.yyyy HH:mm:ss.SSS} | %msg%n</pattern>
                <immediateFlush>true</immediateFlush>
            </encoder>
        </appender>
    </logger>
```

Для логирования статистики *за тайм-фрейм* необходимо настроить логирование класса `ru.open.monitor.statistics.log.FramedStatisticsLogger`, уровень логирования указать в соответствии со свойством `statistics.monitor.log.framedLoggerLevel`.

```xml
    <property name="LOG_PATH" value="${LOG_PATH:-./log}" />
    <logger name="ru.open.monitor.statistics.log.FramedStatisticsLogger" level="DEBUG" additivity="false">
        <appender class="ch.qos.logback.core.rolling.RollingFileAppender">
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/statistics/statistics-framed-${ROLLING_PATTERN}</fileNamePattern>
                <maxHistory>10</maxHistory>
            </rollingPolicy>
            <append>true</append>
            <encoder>
                <pattern>%d{dd.MM.yyyy HH:mm:ss.SSS} | %msg%n</pattern>
                <immediateFlush>true</immediateFlush>
            </encoder>
        </appender>
    </logger>
```

Для логирования статистики обработки пачки событий очередью `CoreEventQueue` необходимо настроить логирование класса `ru.open.monitor.statistics.log.EventBatchStatisticsLogger`, уровень логирования указать в соответствии со свойством `statistics.monitor.log.framedLoggerLevel`.

```xml
    <property name="LOG_PATH" value="${LOG_PATH:-./log}" />
    <logger name="ru.open.monitor.statistics.log.EventBatchStatisticsLogger" level="DEBUG" additivity="false">
        <appender class="ch.qos.logback.core.rolling.RollingFileAppender">
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/statistics/statistics-batch-${ROLLING_PATTERN}</fileNamePattern>
                <maxHistory>10</maxHistory>
            </rollingPolicy>
            <append>true</append>
            <encoder>
                <pattern>%d{dd.MM.yyyy HH:mm:ss.SSS} | %msg%n</pattern>
                <immediateFlush>true</immediateFlush>
            </encoder>
        </appender>
    </logger>
```

Для логирования статистики *за тайм-фрейм* в формате [CSV](http://ru.wikipedia.org/wiki/CSV) необходимо добавить следующие логгеры:

* `ru.open.monitor.statistics.log.FramedStatisticsQueuedLoggerCsv`;
* `ru.open.monitor.statistics.log.FramedStatisticsProcessedLoggerCsv`;
* `ru.open.monitor.statistics.log.FramedStatisticsPublishedLoggerCsv`;
* `ru.open.monitor.statistics.log.FramedStatisticsQueueLoggerCsv`.

```xml
    <property name="LOG_PATH" value="${LOG_PATH:-./log}" />
    <property name="ROLLING_PATTERN_CSV" value="%d{yyyy-MM-dd}.csv.gz" />
    <logger name="ru.open.monitor.statistics.log.FramedStatisticsQueuedLoggerCsv" level="DEBUG" additivity="false">
        <appender class="ru.open.monitor.statistics.log.logback.rolling.ProcessedEventStatisticsRollingFileAppender">
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/statistics/queued-events-${ROLLING_PATTERN_CSV}</fileNamePattern>
                <maxHistory>10</maxHistory>
            </rollingPolicy>
            <append>true</append>
            <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                <layout class="ru.open.monitor.statistics.log.logback.layout.CsvLayout">
                    <lineSeparator>UNIX</lineSeparator>
                </layout>
                <immediateFlush>true</immediateFlush>
            </encoder>
        </appender>
    </logger>
    <logger name="ru.open.monitor.statistics.log.FramedStatisticsProcessedLoggerCsv" level="DEBUG" additivity="false">
        <appender class="ru.open.monitor.statistics.log.logback.rolling.ProcessedEventStatisticsRollingFileAppender">
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/statistics/processed-events-${ROLLING_PATTERN_CSV}</fileNamePattern>
                <maxHistory>10</maxHistory>
            </rollingPolicy>
            <append>true</append>
            <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                <layout class="ru.open.monitor.statistics.log.logback.layout.CsvLayout">
                    <lineSeparator>UNIX</lineSeparator>
                </layout>
                <immediateFlush>true</immediateFlush>
            </encoder>
        </appender>
    </logger>
    <logger name="ru.open.monitor.statistics.log.FramedStatisticsPublishedLoggerCsv" level="DEBUG" additivity="false">
        <appender class="ru.open.monitor.statistics.log.logback.rolling.PublishedEventStatisticsRollingFileAppender">
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/statistics/published-events-${ROLLING_PATTERN_CSV}</fileNamePattern>
                <maxHistory>10</maxHistory>
            </rollingPolicy>
            <append>true</append>
            <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                <layout class="ru.open.monitor.statistics.log.logback.layout.CsvLayout">
                    <lineSeparator>UNIX</lineSeparator>
                </layout>
                <immediateFlush>true</immediateFlush>
            </encoder>
        </appender>
    </logger>
    <logger name="ru.open.monitor.statistics.log.FramedStatisticsQueueLoggerCsv" level="DEBUG" additivity="false">
        <appender class="ru.open.monitor.statistics.log.logback.rolling.QueueStatisticsRollingFileAppender">
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>${LOG_PATH}/statistics/queue-statistics-${ROLLING_PATTERN_CSV}</fileNamePattern>
                <maxHistory>10</maxHistory>
            </rollingPolicy>
            <append>true</append>
            <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
                <layout class="ru.open.monitor.statistics.log.logback.layout.CsvLayout">
                    <lineSeparator>UNIX</lineSeparator>
                </layout>
                <immediateFlush>true</immediateFlush>
            </encoder>
        </appender>
    </logger>
```

Конфигурационные свойства:

* `statistics.monitor.log.rate` -- периодичность вывода *полной* статистики в лог в *миллисекундах* (параметр *необязательный*: значение по умолчанию = 30000);
* `statistics.monitor.log.interestProcessedEvents` -- типы обрабатываемых событий, перечисленных через запятую, по которым статистика должна выводиться в лог (параметр *необязательный*: по умолчанию в лог будет выводиться статистика по всем типам обрабатываемых событий);
* `statistics.monitor.log.interestPublishedEvents` -- типы публикуемых событий, перечисленных через запятую, по которым статистика должна выводиться в лог (параметр *необязательный*: по умолчанию в лог будет выводиться статистика по всем типам публикуемых событий);
* `statistics.monitor.log.fullLoggerLevel` -- уровень логирования для вывода *полной* статистики (параметр *необязательный*: значение по умолчанию = `TRACE`);
* `statistics.monitor.log.framedLoggerLevel` -- уровень логирования для вывода статистики *за промежуток времени* (параметр *необязательный*: значение по умолчанию = `DEBUG`).
