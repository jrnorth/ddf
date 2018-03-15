/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/* global define*/
define([
    'wreqr',
    'application',
    'underscore',
    'marionette',
    'backbone',
    'jquery',
    'properties',
    'maptype',
    'templates/preferences/preferences.modal.handlebars',
    'templates/preferences/layer.preferences.tab.handlebars',
    'templates/preferences/layer.list.handlebars',
    'templates/preferences/layerPicker.handlebars',
    'templates/preferences/preference.buttons.handlebars',
    'component/singletons/user-instance',
    'js/CustomElements',
    'sortablejs'
], function (wreqr, Application, _, Marionette, Backbone, $, properties, maptype,
             preferencesModalTemplate, layerPrefsTabTemplate, layerListTemplate,
             layerPickerTemplate, preferenceButtonsTemplate, user, CustomElements,
             Sortable) {
    var PrefsModalView = Marionette.LayoutView.extend({
        tagName: CustomElements.register('layer-preferences'),
        setDefaultModel: function(){
            this.model = user.get('user>preferences');
        },
        template: preferencesModalTemplate,
        className: 'prefsModal',
        regions: {
            layerTabRegion: '#layerTab'
        },
        initialize: function (options) {
            if (options.model===undefined){
                this.setDefaultModel();
            }
            this.layerTabView = new PrefsModalView.LayerTabView({
                model: this.model.get('mapLayers')
            });
        },
        onRender: function () {
            this.layerTabRegion.show(this.layerTabView);
        }
    });
    PrefsModalView.Buttons = Marionette.LayoutView.extend({
        template: preferenceButtonsTemplate,
        events: {
            'click button.reset-defaults': 'resetDefaults',
        },
        className: 'preferenceTabButtons',
        ui: { save: 'button.save' },
        initialize: function (options) {
            this.tabView = options.tabView;
        },
        resetDefaults: function () {
            this.tabView.resetDefaults();
        }
    });
    PrefsModalView.LayerTabView = Marionette.LayoutView.extend({
        template: layerPrefsTabTemplate,
        regions: {
            layerPickersRegion: '#layerPickers',
            layerButtonsRegion: '#layerButtons'
        },
        initialize: function () {
            this.listenToModel();
        },
        listenToModel: function(){
            this.stopListeningToModel();
            this.listenTo(this.model, 'change:alpha change:show', this.save);
        },
        stopListeningToModel: function(){
            this.stopListening(this.model, 'change:alpha change:show', this.save);
        },
        onRender: function () {
            // HACK fix it
            this.layerPickers = new PrefsModalView.LayerPickerTable({
                childView: PrefsModalView.LayerPicker,
                collection: this.model,
                updateOrdering: this.updateOrdering.bind(this)
                //sort: false
            });
            this.layerButtons = new PrefsModalView.Buttons({ tabView: this });
            this.showLayerPickersAndLayerButtons();
        },
        showLayerPickersAndLayerButtons: function(){
            this.layerPickersRegion.show(this.layerPickers);
            this.layerButtonsRegion.show(this.layerButtons);
        },
        updateOrdering: function() {
            _.forEach(this.$el.find('#pickerList tr'), (element, index) => {
                this.model.get(element.getAttribute('data-id')).set('order', index + 1);
            });
            this.model.sort();
            this.save();
        },
        save: function () {
            this.model.savePreferences();
        },
        resetDefaults: function () {
            this.stopListeningToModel();
            this.model.forEach(function(viewLayer){
                var name = viewLayer.get('name');
                defaultConfig = _.find(properties.imageryProviders, function (layerObj) {
                    return name === layerObj.name;
                });
                viewLayer.set(defaultConfig);
            });
            this.model.sort();
            this.save();
            this.listenToModel();
        }
    });
    /*
     * using CompositeView because it supports table header in template.
    */
    PrefsModalView.LayerPickerTable = Marionette.CompositeView.extend({
        template: layerListTemplate,
        childViewContainer: '#pickerList',
        onRender: function(){
            Sortable.create(this.el.querySelector('#pickerList'), {
                handle: '.layer-rearrange',
                onEnd: () => {
                    this.options.updateOrdering();
                }
            });
        }
    });

    PrefsModalView.LayerPicker = Marionette.ItemView.extend({
        template: layerPickerTemplate,
        tagName: 'tr',
        className: 'layerPicker-row',
        ui: { range: 'input[type="range"]' },
        attributes: function(){
            return {
                'data-id': this.model.id
            };
        },
        initialize: function () {
            this.modelBinder = new Backbone.ModelBinder();
            this.listenTo(this.model, 'change:show', this.changeShow);
            this.listenTo(this.model, 'change:removal', this.changeRemove);
        },
        onRender: function () {
            var layerBindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, layerBindings);
            this.changeShow();
        },
        changeShow: function () {
            if (!this.model.attributes.customLayer) {
                this.$el.find('#layerRemoval').remove();
            }
            this.$el.toggleClass('is-disabled', !this.model.get('show'));
            this.ui.range.prop('disabled', !this.model.get('show'));
        },
        onDestroy: function () {
            this.modelBinder.unbind();
        },
        changeRemove: function() {
            if (this.model.attributes.customLayer) {
                var that = this;
                var map = wreqr.reqres.request('map:retrieval');
                var mapCID = wreqr.reqres.request('map:layerRetrieval');
                var localStorageLayers = window.localStorage.getItem('preferences');

                if (localStorageLayers != null && JSON.parse(localStorageLayers)['mapLayers'] != null) {
                    if (user.get('user').isGuestUser()) {
                        this.removeFromLocalStorageAndUpdate(JSON.parse(localStorageLayers));
                    } 
                }

                user.get('user>preferences>mapLayers')
                    .remove(this.model.id);

                var key = _.findKey(mapCID, function(value, key) {
                    return key.indexOf(that.model.id) >= 0;
                });

                map.removeLayer(mapCID[key]);
            }
        },
        removeFromLocalStorageAndUpdate(localStorageLayers) {
            var that = this;

            var updatedMapLayers = _.without(localStorageLayers.mapLayers, _.findWhere(localStorageLayers.mapLayers, {
                id: that.model.id
            }));

            localStorageLayers.mapLayers = updatedMapLayers
            window.localStorage.setItem('preferences',(JSON.stringify(localStorageLayers)))
        }
    });
    return PrefsModalView;
});
