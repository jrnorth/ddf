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
/*global define*/
const Marionette = require('marionette');
const $ = require('jquery');
const PropertyView = require('component/property/property.view');
const Property = require('component/property/property');
const user = require('component/singletons/user-instance');
const template = require('./metacard-order.hbs');
const CustomElements = require('js/CustomElements');
const announcement = require('component/announcement');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('metacard-ordering'),
    modelEvents: {},
    regions: {
        deliveryPicker: '.delivery-methods-picker',
        submitDelivery: '.submit-delivery'
    },
    events: {
        'click .submit-delivery': 'handleDelivery'
    },
    ui: {},
    initialize: function() {

    },
    onBeforeShow: function() {
        this.turnOffEditing();
        this.setupDeliveryPicker();
        this.turnOnEditing();
    },
    turnOffEditing: function() {
        this.regionManager.forEach(function(region) {
            if (region.currentView) {
                region.currentView.turnOffEditing();
            }
        });
    },
    setupDeliveryPicker: function() {
        let possibleEnumValues = user.get('user').getPreferences().get('deliveryMethods').map(val => ({label: val.get('name'), value: val.get('deliveryId'), class: ''}));
        this.deliveryPicker.show(new PropertyView({
            model: new Property({
                enumFiltering: false,
                showValidationIssues: false,
                enumMulti: true,
                enum: possibleEnumValues,
                value: [[]],
                id: '',
                showLabel: false
            })
        }));
        this.deliveryPicker.currentView.turnOnLimitedWidth();
    },
    turnOnEditing: function() {
        this.$el.addClass('is-editing');
        this.regionManager.forEach(function(region) {
            if (region.currentView) {
                region.currentView.turnOnEditing();
            }
        });
    },
    handleDelivery: function() {
        $.post({
            url: '/search/catalog/internal/delivery',
            data: JSON.stringify({ 
                metacardId: this.model.get('metacardId')[0],
                deliveryIds: this.deliveryPicker.currentView.model.getValue()[0],
                username: user.get('user').get('userid')
            }),
            success: () => { console.log('POST to /delivery successful'); },
            error: () => { 
                console.log('POST to /delivery unsuccessful');
                announcement.announce({
                    title: 'Delivery Failed',
                    message: 'Please verify your delivery configuration and attempt to deliver again.',
                    type: 'error'
                });
            }
        });
        this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox');
    }
});