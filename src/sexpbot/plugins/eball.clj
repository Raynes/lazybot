(ns sexpbot.plugins.eball
  (:use [sexpbot registry]))

(def responses
     ["As I see it, yes."
      "It is certain."
      "It is decidedly so."
      "Most likely." 
      "Outlook good." 
      "Signs point to yes."
      "Without a doubt." 
      "Yes."
      "Yes - definitely."
      "You may rely on it." 
      "Reply hazy, try again."
      "Ask again later."
      "Better not tell you now."
      "Cannot predict now."
      "Concentrate and ask again."
      "Don't count on it."
      "My reply is no."
      "My sources say no."
      "Outlook not so good."
      "Very doubtful."])

(defplugin
  (:cmd
   "Ask the magic eight ball a question."
   #{"8ball"} 
   (fn [{:keys [bot nick channel] :as com-m}]
     (let [answer (rand-int 20)]
       (send-message com-m (prefix bot nick (nth responses answer)))))))