# vertx-metrics

# Hot to reproduce the issue
- open a browser and navigate to http://localhost:8080/ReadStatus.v1
- update vertxVersion in settings.gradle to  3.9.14
- refresh teh browser, when the code in ServerMetrics line 76 get executed the thread is locked for ever.
