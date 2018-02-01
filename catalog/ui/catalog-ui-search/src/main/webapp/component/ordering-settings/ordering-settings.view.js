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
/*global require*/
var Marionette = require('marionette');
var $ = require('jquery');
var template = require('./ordering-settings.hbs');
var CustomElements = require('js/CustomElements');
var OrderingConfigCollectionView = require('../ordering-config/ordering-config.collection.view');
var OrderingSettingsEditor = require('../ordering-settings-editor/ordering-settings-editor');
var OrderingSettingsEditorView = require('../ordering-settings-editor/ordering-settings-editor.view');
var user = require('component/singletons/user-instance');
const uuid = require('uuid');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('ordering-settings'),
    modelEvents: {},
    events: {
        'click > .create-button': 'handleCreateNew',
        'click .save-config': 'handleSaveConfig',
        'click .cancel-edit': 'handleCancelEdit',
        'click .delete-config': 'updateOrderingSettings'
    },
    regions: {
        orderingSettings: '> .current-settings',
        settingsEditor: '> .editor'
    },
    ui: {},

    onRender: function () {
        this.updateOrderingSettings();
        let availableTypes = [];
        $.ajax({
            url:'/search/catalog/internal/digorno',
            async: false,
            success: function (result) {
                availableTypes = result;
            }
        });
        let editorModel = new OrderingSettingsEditor({availableTypes: availableTypes});
        this.settingsEditor.show(new OrderingSettingsEditorView({model: editorModel}));
    },

    handleCreateNew: function () {
        this.$el.toggleClass('editing-config', true);
    },
    
    handleCancelEdit: function () {
        this.$el.toggleClass('editing-config', false);
        let editorView = this.settingsEditor.currentView;
        editorView.regionManager.forEach((region) => {
            region.currentView.revert();
        });
        editorView['configName'].currentView.$el.toggleClass('is-hidden', true);
    },
    
    handleSaveConfig: function () {
        let prefs = user.get('user').getPreferences();
        let config = {fields: []};
        let creds = {};
        let editorView = this.settingsEditor.currentView;
        let configUuid = uuid();
        config['deliveryId'] = configUuid;

        editorView.regionManager.forEach((region) => {
            let prop = region.currentView.model.attributes.label;
            let value = region.currentView.model.getValue()[0];
            if (value) {
                switch (prop) {
                    case 'configType':
                        config[prop] = value;
                        break;
                    case 'Name':
                        config[prop.toLowerCase()] = value;
                        break;
                    case 'Password':
                        config.fields.push({name: prop, value: '********'});
                        creds['credPassword'] = value;
                        break;
                    case 'Username':
                        config.fields.push({name: prop, value: value});
                        creds['credUsername'] = value;
                        break;
                    default:
                        config.fields.push({name: prop, value: value});
                }
            }
        });
        prefs.addOrderingSetting(config);
        prefs.savePreferences();

        if (Object.getOwnPropertyNames(creds).length !== 0) {
            creds['uuid'] = configUuid;
            creds['ddfUsername'] = user.get('user').get('userid');
            this.forwardCredentials(creds);
        }

        editorView.regionManager.forEach((region) => {
            region.currentView.revert();
        });
        editorView['configName'].currentView.$el.toggleClass('is-hidden', true);
        this.$el.toggleClass('editing-config', false);
        this.updateOrderingSettings();
    },
    forwardCredentials: function (creds) {
        $.post({
            url: '/search/catalog/internal/credentials',
            data: JSON.stringify(creds)
        });
    },
    updateOrderingSettings: function() {
        this.orderingSettings.empty();
        let userSettings = user.get('user').getPreferences().get('deliveryMethods');
        this.orderingSettings.show(new OrderingConfigCollectionView(
            {collection: userSettings})
        );

    }
});