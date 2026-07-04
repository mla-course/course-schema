(ns mla.course-schema.import
  "One spec, two views. Export schemas are the source of truth; every
   field the compiler (not the author) is responsible for is tagged
   `{:generated true}` in place, e.g. `[:id {:generated true} :uuid]`.
   `->import-schema` derives the lenient authoring counterpart by
   relaxing exactly those fields to optional — nothing is duplicated,
   so a field added once is correct on both sides forever."
  (:require [malli.core :as m]))

(defn- relax-entry-props
  "Given an entry's own property map (the middle element of a
   `[key props child]` triple), adds `:optional true` when the entry is
   tagged `:generated`. Leaves every other property untouched, and leaves
   non-generated entries byte-for-byte as they were."
  [props]
  (cond-> props (:generated props) (assoc :optional true)))

(defn ->import-schema
  "The import counterpart of a `:map` export schema form: entries marked
   `{:generated true}` (e.g. `[:id {:generated true} :uuid]`) become
   optional in the returned schema; everything else is preserved as-is.

   IMPORTANT: `:generated` is an *entry*-level property, not a property of
   the value schema. For `:map` schemas `m/walk` hands the walker function
   `children` shaped as `[key entry-props child-schema]` triples — that's
   where `:generated` lives. We special-case exactly `:map` (checked via
   `m/type`, not `m/entries` — `:merge` also implements `m/entries` for
   lookup purposes, but its own `children` are whole sub-schemas to merge,
   not entry triples, so treating it like a `:map` here would corrupt it).

   `:schema` and `:ref` are also special-cased, but the opposite way —
   left completely untouched rather than reconstructed via `into-schema`.
   These two show up together whenever a spec defines its own local
   registry for a recursive shape (e.g. `exercise/InlineHiccupContent`'s
   `[:schema {:registry {::node ...}} [:vector [:schema [:ref ::node]]]]`).
   `m/walk` is postorder, so by the time our walker reaches the outer
   `:schema` node it has already recursed into every `:ref` inside it;
   reconstructing a `:ref` via `into-schema` outside the exact registry
   scope that resolves it throws `:malli.core/invalid-ref` — the ref
   keyword is still valid, but `into-schema` has no registry in `options`
   to check it against anymore. Neither `:schema` nor `:ref` ever carries
   a `{:generated true}` *map entry* to relax in the first place (that
   only lives on `:map` entries), so there's nothing to rebuild here —
   returning `s` as-is sidesteps the whole problem for free.

   Everything else — `:and`, `:merge`, `:multi`, `:string`, ... — falls
   through untouched at this node; postorder walking still relaxes any
   `:map` leaves nested inside them."
  [schema]
  (m/walk
   schema
   (fn [s _ children options]
     (cond
       (= :map (m/type s))
       (m/into-schema (m/type s) (m/properties s)
                       (map (fn [[k props child]] [k (relax-entry-props props) child])
                            children)
                       options)

       (#{:schema :ref} (m/type s))
       s

       :else
       (m/into-schema (m/type s) (m/properties s) children options)))))

