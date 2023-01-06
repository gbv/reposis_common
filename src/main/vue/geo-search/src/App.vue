<template>
  <h1>{{ translation.heading }}</h1>
  <p>
    {{ translation.message }}
  </p>
  <p>
    {{ translation.circleMessage }}
  </p>
  <p>
    {{ translation.polygonMessage }}
  </p>
  <GeoSearchMap v-if="model.requestUrl!==''"
                :center-x="model.centerX"
                :center-y="model.centerY"
                :request-url="model.requestUrl"
                :solr-field="model.solrField"
                :zoom="model.zoom"
  />
</template>

<script lang="ts" setup>
import GeoSearchMap from "@/components/GeoSearchMap.vue";
import {onBeforeMount, reactive} from "vue";


const model = reactive({
  centerX: 0,
  centerY: 0,
  zoom: 0,
  requestUrl: "",
  solrField: "",
  roles: [] as string[],
  internalURL: "",
  publicURL: ""
});

const translation = reactive({
  message: "",
  heading: "",
  circleMessage: "",
  polygonMessage: ""
});

const baseURL = (window as any)["webApplicationBaseURL"] as string;
const language = (window as any)["currentLang"] as string;

fetch(baseURL + "rsc/locale/translate/" + language + "/geosearch.message").then(response => response.text())
    .then(data => translation.message = data);

fetch(baseURL + "rsc/locale/translate/" + language + "/geosearch.circle.message").then(response => response.text())
    .then(data => translation.circleMessage = data);

fetch(baseURL + "rsc/locale/translate/" + language + "/geosearch.polygon.message").then(response => response.text())
    .then(data => translation.polygonMessage = data);

fetch(baseURL + "rsc/locale/translate/" + language + "/geosearch.heading").then(response => response.text())
    .then(data => translation.heading = data);

onBeforeMount(async () => {
  console.log("onBeforeMount");
  const config = await fetch(baseURL + "/vue/geo-search/config.json").then(response => response.json());
  model.roles = config["MCR.GeoSearch.Solr.InternalRoles"].split(",");
  model.solrField = config["MCR.GeoSearch.Solr.WKT.Field"];
  model.centerX = config["MCR.GeoSearch.Solr.Map.CenterX"];
  model.centerY = config["MCR.GeoSearch.Solr.Map.CenterY"];
  model.zoom = config["MCR.GeoSearch.Solr.Map.Zoom"];

  model.internalURL = config["MCR.GeoSearch.Solr.Internal.SearchURI"];
  model.publicURL = config["MCR.GeoSearch.Solr.Public.SearchURI"];

  const result = await fetch(baseURL + "rsc/jwt").then(response => response.text());
  const jwt = result.split(".")[1];
  const decodedJwt = JSON.parse(atob(jwt));
  const roles = decodedJwt["mcr:roles"] as string[];
  model.requestUrl = ((window as any)["webApplicationBaseURL"] as string);

  if (roles.some(role => model.roles.includes(role))) {
    model.requestUrl += model.internalURL.replace("$USERNAME", decodedJwt["sub"]);
  } else {
    model.requestUrl += model.publicURL.replace("$USERNAME", decodedJwt["sub"]);
  }
});

</script>

<style>

</style>
