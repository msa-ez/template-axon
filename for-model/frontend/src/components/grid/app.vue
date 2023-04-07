<template>
    <div class="container-fluid">
        <div class="row">
            <!-- search box -->
            <div class="toolbar-item col-sm-3 col-md-5">
                <wj-flex-grid-search
                    placeholder="Search"
                    cssMatch=""
                    :initialized="searchInitialized"
                />
            </div>

            <!-- data size -->
            <div class="toolbar-item col-sm-3 col-md-3">
                <div class="input-group">
                    <span class="input-group-addon">Items:</span>
                    <select class="form-control" v-model="itemsCount">
                        <option value="5">5</option>
                        <option value="50">50</option>
                        <option value="500">500</option>
                        <option value="5000">5,000</option>
                        <option value="50000">50,000</option>
                        <option value="100000">100,000</option>
                    </select>
                </div>
            </div>

            <!-- export to Excel -->
            <div class="toolbar-item col-sm-3 col-md-2">
                <excel-export-button :exportService="this.exportService" :getFlex="getFlex" />
            </div>

            <!-- export to PDF -->
            <div class="toolbar-item col-sm-3 col-md-2">
                <button class="btn btn-default btn-block" @click="exportToPdf">Export To PDF</button>
            </div>
        </div>

        <!-- group panel -->
        <wj-group-panel :placeholder="'Drag columns here to create groups'" :initialized="groupPanelInitialized" />

        <!-- the grid -->
        <wj-flex-grid
                :autoGenerateColumns="false"
                :allowAddNew="true"
                :allowDelete="true"
                :allowPinning="'SingleColumn'"
                :newRowAtTop="true"
                :showMarquee="true"
                :selectionMode="'MultiRange'"
                :validateEdits="false"
                :initialized="gridInitialized"
        >

            <wj-flex-grid-filter
                    :filterColumns="['id', 'date', 'time', 'countryId', 'productId', 'colorId', 'price', 'change', 'discount', 'rating', 'active']" />

            <wj-flex-grid-column binding="id" header="ID" :width="70" :isReadOnly="true" />
            <wj-flex-grid-column binding="date" header="Date" format="MMM d yyyy" :isRequired="false" :width="130"
                :editor="editors.inputDate">
            </wj-flex-grid-column>
            <wj-flex-grid-column binding="countryId" header="Country" :dataMap="countryMap" :width="145">
                <wj-flex-grid-cell-template cellType="Cell" v-slot="cell">
                    <span :class="'flag-icon flag-icon-' + getCountry(cell.item).flag"></span>
                    {{getCountry(cell.item).name}}
                </wj-flex-grid-cell-template>
            </wj-flex-grid-column>
            <wj-flex-grid-column binding="price" header="Price" format="c" :isRequired="false" :width="100"/>
            <wj-flex-grid-column binding="history" header="History" :width="180" align="center" 
                :allowSorting="false" :cellTemplate="historyCellTemplate">
            </wj-flex-grid-column>
            <wj-flex-grid-column binding="change" header="Change" align="right" :width="115">
                <wj-flex-grid-cell-template cellType="Cell" v-slot="cell">
                <span :class="getChangeCls(cell.item.change)">
                    {{cell.item.change | safeCurrency}}
                </span>
                </wj-flex-grid-cell-template>
            </wj-flex-grid-column>
            <wj-flex-grid-column binding="rating" header="Rating" :width="180" align="center" cssClass="cell-rating"
                                 :cellTemplate="ratingCellTemplate">
            </wj-flex-grid-column>
            <wj-flex-grid-column binding="time" header="Time" format="HH:mm" :isRequired="false" :width="95"
                :editor="editors.inputTime">
            </wj-flex-grid-column>
            <wj-flex-grid-column binding="colorId" header="Color" :dataMap="colorMap" :width="145">
                <wj-flex-grid-cell-template cellType="Cell" v-slot="cell">
                    <span class="color-tile" :style="{background: getColor(cell.item).value}"></span>
                    {{getColor(cell.item).value}}
                </wj-flex-grid-cell-template>
            </wj-flex-grid-column>
            <wj-flex-grid-column binding="productId" header="Product" :dataMap="productMap" :width="145" />
            <wj-flex-grid-column binding="discount" header="Discount" format="p0" :width="130" />
            <wj-flex-grid-column binding="active" header="Active" :width="100" />

        </wj-flex-grid>
    </div>
</template>

<script>
import '@grapecity/wijmo.touch';
import '@grapecity/wijmo.vue2.grid';
import '@grapecity/wijmo.vue2.grid.grouppanel';
import '@grapecity/wijmo.vue2.grid.filter'
import "@grapecity/wijmo.vue2.grid.search";
import '@grapecity/wijmo.vue2.input';
import * as wjCore from '@grapecity/wijmo';
import { InputDate, InputTime } from "@grapecity/wijmo.input";
import { DataMap } from '@grapecity/wijmo.grid';
import { CellMaker, SparklineMarkers } from '@grapecity/wijmo.grid.cellmaker';
import { Country, DataService, KeyValue } from "./data";
import { ExportService } from "./export";

export default {
    el: "#app",
    data: function() {
        return {
            itemsCount: 500,

            // editors
            editors: {
                inputDate: new InputDate(document.createElement('div'), {
                    format: 'MM/dd/yyyy',
                    isRequired: false
                }),
                inputTime: new InputTime(document.createElement('div'), {
                    format: 'HH:mm',
                    isRequired: false
                })
            }
        };
    },
    created: function () {
        this.dataService = new DataService();
        this.exportService = new ExportService();
        this._lastId = this.itemsCount;
        // initializes data maps
        this._productMap = this._buildDataMap(this.dataService.getProducts());
        this._countryMap = new DataMap(this.dataService.getCountries(), 'id', 'name');
        this._colorMap = this._buildDataMap(this.dataService.getColors());
        // initializes cell templates
        this.historyCellTemplate = CellMaker.makeSparkline({
            markers: SparklineMarkers.High | SparklineMarkers.Low,
            maxPoints: 25,
            label: 'price history',
        });
        this.ratingCellTemplate = CellMaker.makeRating({
            range: [1, 5],
            label: 'rating'
        });
    },
    beforeDestroy() {
        this.exportService.cancelExcelExport();
    },
    methods: {
        gridInitialized: function (ctl) {
            this.flex = ctl;
            this.flex.itemsSource = this._createItemsSource(this.itemsCount);
            if (this.groupPanel) {
                this.groupPanel.grid = this.flex;
            }
            if (this.search) {
                this.search.grid = this.flex;
            }
        },
        searchInitialized: function (ctl) {
            this.search = ctl;
            if (this.flex) {
                this.search.grid = this.flex;
            }
        },
        groupPanelInitialized: function (ctl) {
            this.groupPanel = ctl;
            if (this.flex) {
                this.groupPanel.grid = this.flex;
            }
        },
        exportToPdf: function() {
            this.exportService.exportToPdf(this.flex, {
                countryMap: this._countryMap,
                colorMap: this._colorMap,
                historyCellTemplate: this.historyCellTemplate
            });
        },
        getCountry: function (item) {
            const country = this._countryMap.getDataItem(item.countryId);
            return country ? country : Country.NotFound;
        },
        getColor: function (item) {
            const color = this._colorMap.getDataItem(item.colorId);
            return color ? color : KeyValue.NotFound;
        },
        getChangeCls: function (value) {
            if (wjCore.isNumber(value)) {
                if (value > 0) {
                    return 'change-up';
                }
                if (value < 0) {
                    return 'change-down';
                }
            }
            return '';
        },
        _createItemsSource(counter) {
            const data = this.dataService.getData(counter);
            const view = new wjCore.CollectionView(data, {
                getError: (function (item, prop) {
                    const displayName = this.flex.columns.getColumn(prop).header;
                    return this.dataService.validate(item, prop, displayName);
                }).bind(this)
            });
            view.collectionChanged.addHandler((function (s, e) {
                // initializes new added item with a history data
                if (e.action === wjCore.NotifyCollectionChangedAction.Add) {
                    e.item.history = this.dataService.getHistoryData();
                    e.item.id = this._lastId;
                    this._lastId ++;
                }
            }).bind(this));
            return view;
        },
        // build a data map from a string array using the indices as keys
        _buildDataMap: function (items) {
            const map = [];
            for (let i = 0; i < items.length; i++) {
                map.push({ key: i, value: items[i] });
            }
            return new DataMap(map, 'key', 'value');
        },
        getFlex: function () {
            return this.flex;
        },
    },
    computed: {
        productMap: function () {
            return this._productMap;
        },
        countryMap: function () {
            return this._countryMap;
        },
        colorMap: function () {
            return this._colorMap;
        },
    },
    watch: {
        itemsCount: function (itemsCount) {
            this.flex.itemsSource.collectionChanged.removeAllHandlers();

            this._lastId = itemsCount;
            this.flex.itemsSource = this._createItemsSource(itemsCount);
        },
    },
    filters: {
        safeCurrency: function (value) {
            if (wjCore.isNumber(value)) {
                return wjCore.Globalize.formatNumber(value, 'c');
            }
            if (!wjCore.isUndefined(value) && value !== null) {
                return wjCore.changeType(value, wjCore.DataType.String);
            }
            return '';
        }
    },
}
</script>

<style>
    @import "~@grapecity/wijmo.styles/wijmo.css";
    @import "~bootstrap/dist/css/bootstrap.css";
    @import url('https://cdnjs.cloudflare.com/ajax/libs/flag-icon-css/3.1.0/css/flag-icon.css');

    body {
        font-size:1.5em;
        font-family: -apple-system,BlinkMacSystemFont,"Segoe UI Light",Roboto,Oxygen-Sans,Ubuntu,Cantarell,"Helvetica Neue",sans-serif;
    }

    .toolbar-item {
        margin-bottom: 6px;
    }

    .wj-flexgridsearch {
        width: 100%;
    }

    .wj-flexgrid {
        height: 330px;
    }

    .wj-flexgrid .wj-cell {
        padding: 7px;
        border:none;
    }

    .wj-cell.wj-state-invalid:not(.wj-header)::after {
        top: -14px;
        border: 14px solid transparent;
        border-right-color: red;
    }

    .flag-icon {
        box-shadow: 1px 1px 4px rgba(0,0,0,0.4);
    }

    .color-tile {
        display: inline-block;
        position: relative;
        width: 1em;
        height: 1em;
        border-radius: 50%;
        box-shadow: 1px 1px 4px rgba(0,0,0,0.4);
        vertical-align: middle;
    }

    .change-up {
        color: darkgreen;
    }
    .change-up:after {
        content: '\25b2';
    }
    .change-down {
        color: darkred;
    }
    .change-down:after {
        content: '\25bc';
    }

    .cell-rating {
        font-size: 12px;
    }

    .wj-flexgrid .wj-detail {
        padding: 4px 16px;
    }

    .wj-detail h3 {
        margin:10px 0;
    }
</style>
