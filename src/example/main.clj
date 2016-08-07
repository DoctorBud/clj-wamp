(ns example.main
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clj-wamp.server-v2 :as wamp]
            [compojure.handler :refer [site]]
            [compojure.core :as cc :refer [GET POST routes]]
            [org.httpkit.server :refer [run-server]]
            [clojure.pprint :refer [pprint]]))

(defn auth-secret [sess-id auth-key extra]
  (let [result  (string/reverse auth-key)]
    (println "auth-secret" sess-id auth-key extra result)
    result))


(def base-api-url "")

(def origin-re #".*")

; Topic URIs
(defn rpc-url [path] (str base-api-url "api." path))
(defn evt-url [path] (str base-api-url "event." path))

(defn auth-permissions [sess-id auth-key]
  {:rpc         {(rpc-url "broadcast") true}
   :subscribe   {(evt-url "broadcast") true}
   :publish     {(evt-url "broadcast") true}})

(defn on-open-handler [{:keys [events] :as config} ring-id sess-id]
  (println "on-open-handler" sess-id)
  ; This would be a good place to track the sess-id and associate it with
  ; the session.
  )

(defn on-close-handler [{:keys [events] :as config} sess-id reason]
  (println "on-close-handler"))

(defn broadcast-handler [config args msg-packet]
  (let [msg-text  (:msg msg-packet)]
    (println "broadcast-handler" args msg-text)
    (wamp/broadcast-event! (evt-url "broadcast") [{:msg msg-text}] [])
    {:result "ignored RPC result"}))

(defn wamp-rpcs [{:keys [session-store] :as config}]
  {(rpc-url "broadcast")             (partial broadcast-handler config)})

(defn wamp-subscribable-evt [{:keys [session-store] :as config}]
  {(evt-url "broadcast") true})

(defn wamp-publishable-evt [{:keys [session-store] :as config}]
  {(evt-url "broadcast") true})


(defn wamp-handler [config]
  (println "wamp-handler" config)
  (fn [req]
    (println "wamp-handler fn" req)
    (let [ring-id   (:value ((:cookies req) "ring-session"))]


      (let [ch      (:async-channel req)
            body    (wamp/http-kit-handler ch
                            {:on-auth {:allow-anon?  true
                                       :timeout      60000            ; default is 20000 (20 secs)
                                       :secret       auth-secret
                                       :permissions  auth-permissions}
                             :on-open       (partial on-open-handler config ring-id)
                             :on-close      (partial on-close-handler config)
                             :on-call       (wamp-rpcs config)
                             :on-subscribe  (wamp-subscribable-evt config)
                             :on-publish    (wamp-publishable-evt config)
                           })]
      (wamp/with-channel-validation req channel origin-re
        body)))))

(defn web-routes [wamp-handler]
  (routes
      (GET "/ws" req (let [wh (wamp-handler req)]
                        wh))
      (GET "/ws" req
        (do
          (println "/ws" req)
          (wamp-handler req)))
      (GET "/" req (do
                      (println "BOOM")
                      "BOOM"))))

(defn start-server []
  (println "start-server")
  (let [config        {:foo "foo"}
        wshandler     (wamp-handler config)
        web-handler   (web-routes wshandler)
        the-app       (site web-handler)]
    (println "run-server" the-app)
    (run-server the-app {:port 3000 :thread 12})
    )
  )

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-p" "--port PORT" "Port number (NYI)"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

   ;; If no required argument description is given, the option is assumed to
   ;; be a boolean option defaulting to nil
   [nil "--detach" "Detach from controlling process (NYI)"]

   ["-v" nil "Verbosity level; may be specified multiple times to increase value (NYI)"
    ;; If no long-option is specified, an option :id must be given
    :id :verbosity
    :default 0
    ;; Use assoc-fn to create non-idempotent options
    :assoc-fn (fn [m k _] (update-in m [k] inc))]

   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["This is an example of a WS Server."
        ""
        "Usage: lein run [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start    Start a new server"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (println "-main called with:")
    (pprint arguments)
    (pprint options)
    (cond
      (:help options) (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    ;; TODO: have this start the system
    (case (first arguments)
      "start"
       (try
          (start-server)
          (catch Exception e
            (println "System Startup Exception" e)))
      (exit 1 (usage summary)))))

(defn maindummy [& args]
  (println "maindummy called with:")
  (exit 1 "maindummy exited"))


