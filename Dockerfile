FROM clojure

COPY .  /app
WORKDIR /app

RUN lein deps

COPY .lazybot/ /root/.lazybot
COPY example.policy /root/.java.policy
