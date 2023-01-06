<template>
  <div class="geo-search-map">

    <div class="search-type-select">
      <div class="btn-group" role="group">
        <button :class="'btn btn-secondary' + (searchType==='Circle'?' active':'')" type="button"
                v-on:click.prevent="searchType='Circle'"><i class="fas fa-search"> </i>
          {{ translation.circle }}
        </button>
        <button :class="'btn btn-secondary'+ (searchType==='Polygon'?' active':'')" type="button"
                v-on:click.prevent="searchType='Polygon'"><i class="fas fa-search"> </i>
          {{ translation.polygon }}
        </button>
      </div>
    </div>
    <ol-map :loadTilesWhileAnimating="true" :loadTilesWhileInteracting="true" style="height:400px">

      <ol-view ref="view" :center="transform([$props.centerX, $props.centerY], 'EPSG:4326', 'EPSG:3857')" :rotation="0"
               :zoom="$props.zoom"/>

      <ol-tile-layer>
        <ol-source-osm/>
      </ol-tile-layer>

      <ol-vector-layer>
        <ol-source-vector :projection="projection">
          <ol-interaction-draw v-if="!!searchType" :type="searchType" @drawend="drawend">

          </ol-interaction-draw>

        </ol-source-vector>
        <ol-style>
          <ol-style-stroke :width="2" color="red"></ol-style-stroke>
          <ol-style-fill color="rgba(255,255,255,0.1)"></ol-style-fill>
          <ol-style-circle :radius="7">
            <ol-style-fill color="blue"></ol-style-fill>
          </ol-style-circle>
        </ol-style>
      </ol-vector-layer>

      <ol-vector-layer>
        <ol-source-vector
            :format="format"
            :url="docsURL()"
        />
      </ol-vector-layer>

    </ol-map>
  </div>
</template>

<script lang="ts" setup>

import {ref, defineProps, Ref, reactive} from 'vue'
import {transform, getPointResolution} from 'ol/proj';
import {toStringXY} from "ol/coordinate";
import {WKT} from "ol/format";
import {get as getProjection} from "ol/proj";
import {Polygon} from "ol/geom";

const args = defineProps({
  centerX: Number,
  centerY: Number,
  zoom: Number,
  requestUrl: String,
  solrField: String
});

const translation = reactive({circle: "", polygon: ""});

const baseURL = (window as any)["webApplicationBaseURL"] as string;
const language = (window as any)["currentLang"] as string;

fetch(baseURL + "rsc/locale/translate/" + language + "/geosearch.circle").then(response => response.text())
    .then(data => translation.circle = data);
fetch(baseURL + "rsc/locale/translate/" + language + "/geosearch.polygon").then(response => response.text())
    .then(data => translation.polygon = data);

const searchType: Ref<Boolean | String> = ref(false);

const format = new WKT({splitCollection: true});

(format as any).dataProjection = getProjection("EPSG:4326");

function drawend(event: any) {
  if (event.type == "drawend") {
    if (searchType.value == "Circle") {
      const transformedCoords = transform(event.feature.getGeometry().getCenter(), 'EPSG:3857', 'EPSG:4326');
      const transformedRadius = getPointResolution('EPSG:3857', event.feature.getGeometry().getRadius(), event.feature.getGeometry().getCenter(), 'm') / 1000;
      window.location.assign(args.requestUrl + `&pt=${encodeURIComponent(toStringXY([transformedCoords[1], transformedCoords[0]], 6))}&d=${transformedRadius}&fq=${encodeURIComponent("{!geofilt sfield=" + args.solrField + "}")}`);
    } else if (searchType.value == "Polygon") {
      const translatedCoordinates = event.feature.getGeometry().transform('EPSG:3857', 'EPSG:4326').getCoordinates()[0];
      if (!isPolygonInverse(translatedCoordinates)) {
        console.log("Polygon is not inverse")
        translatedCoordinates.reverse();
      }

      const polygonWKT = format.writeGeometry(new Polygon([translatedCoordinates]));
      window.location.assign(args.requestUrl + '&fq=' + encodeURIComponent('{!field f=' + args.solrField + '}Intersects(' + polygonWKT + ')'));
    }
  }
}

function docsURL() {
  return (args.requestUrl as string).replace(/&rows=[0-9]+/, '') + "&rows=9999&fl=" + args.solrField + "&wt=xml&XSL.Transformer=response-WKT";
}

function isPolygonInverse(vertices: number[][]): boolean {
  // Calculate the sum of the angles between the points
  const n = vertices.length;
  let angleSum = 0;
  for (let i = 0; i < n; i++) {
    const p1 = vertices[i];
    const p2 = vertices[(i + 1) % n];
    const p3 = vertices[(i + 2) % n];
    angleSum += angleBetween(p1, p2, p3);
  }

  // If the angle sum is positive, the polygon is counterclockwise
  if (angleSum > 0) {
    return false;
  }
  // If the angle sum is negative, the polygon is clockwise
  else if (angleSum < 0) {
    return true;
  }
  // If the angle sum is zero, the polygon is flat
  else {
    return false;
  }
}

function angleBetween(p1: number[], p2: number[], p3: number[]): number {
  // Calculate the angle between three points
  const a = p1[0] - p2[0];
  const b = p1[1] - p2[1];
  const c = p3[0] - p2[0];
  const d = p3[1] - p2[1];
  return Math.atan2(a * d - b * c, a * c + b * d);
}

</script>

<style scoped>

</style>
