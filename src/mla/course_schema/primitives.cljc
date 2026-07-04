(ns mla.course-schema.primitives)

(def Locale
  "BCP-47 locale tag, e.g. :en, :zh-CN, :fr-FR."
  [:and
   :keyword
   [:fn {:error/message "must be a BCP-47 locale keyword (e.g. :en, :zh-CN)"}
    (fn [k]
      (boolean (re-matches #"^[a-z]{2,3}(?:-[A-Za-z]{4})?(?:-[A-Z]{2}|\d{3})?$"
                           (name k))))]])

(def NonBlankStr
  [:re {:error/message "must not be blank"}
   #".*\S.*"])

(def URL
  [:re {:error/message "must be a valid http(s) URL"}
   #"^https?://\S+$"])

(def DateTime
  [:re {:error/message "must be an ISO-8601 date-time string"}
   #"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$"])

(def License
  [:or NonBlankStr
   [:map
    [:url URL]
    [:name {:optional true} NonBlankStr]]])

(def Attribution
  [:or NonBlankStr
   [:map
    [:url URL]
    [:name {:optional true} NonBlankStr]]])

(def SemVer
  [:re {:error/message "must be a semantic version string (e.g. \"1.0.0\")"}
   #"^\d+\.\d+\.\d+(?:[-+].+)?$"])
