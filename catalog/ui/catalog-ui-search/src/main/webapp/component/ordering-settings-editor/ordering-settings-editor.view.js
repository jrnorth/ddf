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
var template = require('./ordering-settings-editor.hbs');
var CustomElements = require('js/CustomElements');
var PropertyView = require('../property/property.view');
var Property = require('../property/property');
var _ = require('lodash');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('ordering-settings-editor'),
    modelEvents: {},
    regions: {
        typeDropdown: '> .type-selector > .type-dropdown',
        configName: '> .fields > .config-name'
    },
    onRender: function () {
        this.turnOffEditing();
        let availableTypes = this.model.get('availableTypes');

        const types = availableTypes.map(type => ({label: type.displayName, value: type.deliveryType, class: ''}));

        const fieldsMap = availableTypes
            .map(type => type.requiredFields)
            .reduce((accumulator, next) => Object.assign(accumulator, next), {});
        for (const [parameterKey, parameterType] of Object.entries(fieldsMap)) {
            const keyClass = this.toClass(parameterKey);
            this.$el.find('.fields')
                .append('<div class="field-' + keyClass.toLowerCase() + ' type-' + parameterType.toLowerCase() + '"></div>');
            this.addRegion(keyClass, '.field-' + keyClass.toLowerCase());

            let propertyModel = new Property({
                label: parameterKey,
                value: [''],
                type: parameterType.toUpperCase()
            });

            this[keyClass].show(new PropertyView({model: propertyModel}));
            this[keyClass].currentView.turnOnLimitedWidth();
            if (parameterType.match(/password/i)) {
                this[keyClass].currentView.$el.find('input').attr('type', 'password');
            }
            this[keyClass].currentView.$el.toggleClass('is-hidden', true);
        }

        this.typeDropdown.show(new PropertyView({
            model: new Property({
                label: 'deliveryType',
                enumFiltering: false,
                showValidationIssues: false,
                showLabel: false,
                enumMulti: false,
                enum: types,
                value: ["Select a delivery type"]
            })
        }));
        this.typeDropdown.currentView.turnOnLimitedWidth();

        let nameFieldModel = new Property({
            label: 'name',
            value: [''],
            type: 'STRING'
        });
        this.configName.show(new PropertyView({model: nameFieldModel}));
        this.configName.currentView.turnOnLimitedWidth();
        this.configName.currentView.$el.toggleClass('is-hidden', true);

        this.listenTo(this.typeDropdown.currentView.model, 'change:value', _.bind(this.updateFields, this));
        this.turnOnEditing();
    },
    turnOnEditing: function() {
        this.$el.addClass('is-editing');
        this.regionManager.forEach(function (region) {
            if (region.currentView) {
                region.currentView.turnOnEditing();
            }
        });
    },
    turnOffEditing: function() {
        this.regionManager.forEach(function (region) {
            if (region.currentView) {
                region.currentView.turnOffEditing();
            }
        });
    },
    updateFields: function (model, value) {
        this.model.set('selectedType', value);
        const strValue = value[0];
        let fieldsToDisplay = this.model.get('availableTypes').find(type => type.deliveryType === strValue);

        this.regionManager.forEach((region) => {
            region.currentView.$el.toggleClass('is-hidden', true);
        });

        fieldsToDisplay = fieldsToDisplay ?
            Object.keys(fieldsToDisplay.requiredFields).map(this.toClass) : [];
        fieldsToDisplay.push('typeDropdown');
        fieldsToDisplay.push('configName');

        fieldsToDisplay.forEach((regionName) => {
            if (this[regionName]) {
                this[regionName].currentView.$el.toggleClass('is-hidden', false);
            }
        });
    },

    toClass: function(name) {
        return name.replace(/ /g, '-');
    }
});
