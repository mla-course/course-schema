(ns mla.course-schema.migration
  "Pure course-data migration pipeline.

   Design

   • Each migration is a plain fn: course-map → course-map.
   • Migrations are keyed by the version they CONSUME (the 'from' version).
     Applying migrations[0] produces a v1 map, migrations[1] produces v2, etc.
   • `migrate` composes only the fns needed to reach CURRENT_VERSION —
     no work is done for already-current data.
   • All fns are pure; no I/O, no side effects.

   Adding a new version

   1. Increment CURRENT_VERSION in schema.
   2. Add a fn `migrate-vN->vN+1` below.
   3. Register it in `migrations` under key N.
   4. Update the Malli Course schema if the structure changed.
   5. Optionally add an example in the tests.

   Each migration fn receives a map that has already passed through all
   prior migrations, so it can assume the previous version's shape.
   It must return a map with :version set to (inc N)."
  (:require [mla.course-schema.version :as v]))

;;; Version detection

(defn detect-version
  "Return the declared version of a raw parsed course map as an integer.
   A missing :version key means the file pre-dates versioning → treated as 0.
   A non-integer :version value is also treated as 0 (defensive)."
  [raw]
  (let [v (:version raw)]
    (if (integer? v) v 0)))

(def ^:private migrations
  "Map of {from-version → migration-fn}: {N migrate-vN->vN+1}."
  {})

(def ^:private rollbacks
  "Map of {from-version → rollback-fn}, the inverse of `migrations`:
   {N migrate-vN->vN-1}. Only add an entry when downgrading is needed and
   possible without silent data loss."
  {})

(defn- run-steps
  "Thread `course` through `registry`'s fn for each version in `steps`, stamping
   `:version` with `(next-version step)` after each one. Throws a descriptive
   ex-info if a step is unregistered."
  [registry next-version steps course]
  (reduce (fn [c step]
            (if-let [f (get registry step)]
              (assoc (f c) :version (next-version step))
              (throw (ex-info (str "No migration registered for v" step
                                   " in mla.course-schema.migration.")
                              {:step step :registered (keys registry)}))))
          course
          steps))

(defn- result [course from to]
  {:course       course
   :migrated?    (not= from to)
   :from-version from
   :to-version   to})

(defn migrate
  "Migrate `course` from its detected version up to CURRENT_VERSION.
   Returns {:course :migrated? :from-version :to-version}. Throws ex-info if a
   step is missing (a programming error) or the data is newer than this schema."
  [course]
  (let [from   (detect-version course)
        target v/CURRENT_VERSION]
    (when (> from target)
      (throw (ex-info (str "Course is v" from ", newer than supported v" target)
                      {:from from :target target})))
    (result (run-steps migrations inc (range from target) course) from target)))

(defn rollback
  "Downgrade `course` from its detected version down to `target`
   (0 <= target <= detected version). Mirrors `migrate`; see `rollbacks`."
  [course target]
  (let [from (detect-version course)]
    (when-not (<= 0 target from)
      (throw (ex-info (str "Cannot roll back v" from " to v" target)
                      {:from from :target target})))
    (result (run-steps rollbacks dec (range from target -1) course) from target)))
