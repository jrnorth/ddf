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
/*global define, setTimeout*/
define([
    'marionette',
    'backbone',
    'underscore',
    'jquery',
    './query-schedule.hbs',
    'js/CustomElements',
    'js/store',
    'properties',
    'component/property/property.view',
    'component/property/property',
    'component/dropdown/dropdown.view',
    'component/radio/radio.view',
    'component/singletons/user-instance',
    'moment',
    'js/Common'
], function(Marionette, Backbone, _, $, template, CustomElements, store, properties, PropertyView, Property,
    DropdownView, RadioView, user, Moment, Common) {

    function getHumanReadableDuration(milliseconds) {
        var duration = Moment.duration(milliseconds);
        var days = duration.days();
        var hours = duration.hours();
        var minutes = duration.minutes();
        var seconds = duration.seconds();
        var result = days ? (days + ' day(s) ') : '';
        result += hours ? (hours + ' hour(s) ') : '';
        result += minutes ? (minutes + ' minute(s) ') : '';
        result += seconds ? (seconds + ' second(s)') : '';
        return result.trim();
    }

    var pollingFrequencyEnum = properties.scheduleFrequencyList.sort(function(a, b) {
        return a - b;
    }).reduce(function(options, option) {
        var durationInMilliseconds = option * 1000;
        options.push({
            label: getHumanReadableDuration(durationInMilliseconds),
            value: durationInMilliseconds
        });
        return options;
    }, [{
        label: 'Never',
        value: false
    }]);

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-schedule'),
        modelEvents: {},
        regions: {
            enableScheduling: '.enable-scheduling',
            scheduleProperties: '.schedule-properties',
            amountPicker: '.amount-picker',
            unitPicker: '.unit-picker',
            startPicker: '.start-picker',
            endPicker: '.end-picker',
            deliveryPicker: '.delivery-methods-picker'
        },
        ui: {},
        initialize: function(){
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
        },
        onBeforeShow: function() {
            this.turnOffEditing();
            this.setupRegions();
            this.listenTo(this.enableScheduling.currentView.model, 'change:value', this.handleSchedulingValue);
            this.listenTo(this.amountPicker.currentView.model, 'change:value', this.handleAmountPickerValue);
            this.turnOnEditing();
        },

        setupRegions: function() {
            this.enableScheduling.show(new PropertyView({
                model: new Property({
                    value: [this.model.get('isScheduled')],
                    id: 'Schedule',
                    radio: [{
                        label: 'On',
                        value: true,
                        title: 'Activate Query Scheduling'
                    }, {
                        label: 'Off',
                        value: false,
                        title: 'Deactivate Query Scheduling'
                    }]
                })
            }));
            this.enableScheduling.currentView.turnOnLimitedWidth();
            this.handleSchedulingValue();

            this.amountPicker.show(new PropertyView({
                model: new Property({
                    value: [this.model.get('scheduleAmount')],
                    id: '',
                    type: 'INTEGER',
                    showValidationIssues: false,
                    showLabel: false
                })
            }));
            this.amountPicker.currentView.turnOnLimitedWidth();

            this.unitPicker.show(new PropertyView({
                model: new Property({
                    value: [this.model.get('scheduleUnit')],
                    id: '',
                    enum: [{
                        label: 'Months', 
                        value: 'months'
                    }, {
                        label: 'Weeks',
                        value: 'weeks'
                    }, {
                        label: 'Days',
                        value: 'days'
                    }, {
                        label: 'Hours',
                        value: 'hours'
                    }, {
                        label: 'Minutes',
                        value: 'minutes'
                    }],
                    showValidationIssues: false,
                    showLabel: false
                })
            }));
            this.unitPicker.currentView.turnOnLimitedWidth();

            this.startPicker.show(new PropertyView({
                model: new Property({
                    value: [this.model.get('scheduleStart')],
                    id: 'Starts on',
                    placeholder: 'Date and time for query to take effect',
                    type: 'DATE'
                })
            }));
            this.startPicker.currentView.turnOnLimitedWidth();

            this.endPicker.show(new PropertyView({
                model: new Property({
                    value: [this.model.get('scheduleEnd')],
                    id: 'Ends on',
                    placeholder: 'Date and time for query to stop',
                    type: 'DATE'
                })
            }));
            this.endPicker.currentView.turnOnLimitedWidth();

            //let currentDeliveryValues = this.model.get('deliveryIds').map(deliveryId => ({ label: deliveryId, value: deliveryId, class: '' }));
            // let possibleEnumValues = ['myFTP', 'myEmail', 'myPhysicalMailingService'].map(val => ({label: val, value: val, class: ''})); 
            // let possibleEnumValues = {};
            let possibleEnumValues = user.get('user').getPreferences().get('deliveryMethods').map(val => ({label: val.userSpecifiedName, value: val.deliveryId, class: ''}));
            
            // let possibleEnumValues = ['myFTP', 'myEmail', 'myPhysicalMailingService'].map(val => ({label: val, value: val, class: ''}));
            this.deliveryPicker.show(new PropertyView({
                model: new Property({
                    enumFiltering: false,
                    showValidationIssues: false,
                    enumMulti: true,
                    enum: possibleEnumValues,
                    value: [this.model.get('deliveryIds')],
                    id: 'Delivery Method'
                })
            }));
            this.deliveryPicker.currentView.turnOnLimitedWidth();

            // var deliveryMethods = [{ label: 'All Available Methods', value: 'all-methods' }].concat([{ label: 'Email', value: 'joshua.mack@connexta.com'}]);
            // this.deliveryPicker.show(DropdownView.createSimpleDropdown({
            //     list: mappedMethods,
            //     defaultSelection: [mappedMethods[0]] || ['No Delivery Options Available'],
            //     isMultiSelect: true
            // }));
        },
        handleSchedulingValue: function() {
            var isScheduling = this.enableScheduling.currentView.model.getValue()[0];
            this.$el.toggleClass('is-scheduled', isScheduling);
        },
        handleAmountPickerValue: function() {
            var currVal = this.amountPicker.currentView.model.getValue()[0];
            if (currVal < 0) {
                console.log('Hey currval is less than 0. changing model value to be [0]');
                this.amountPicker.currentView.model.setValue([0]);
            }
        },
        turnOnEditing: function() {
            this.$el.addClass('is-editing');
            this.regionManager.forEach(function(region) {
                if (region.currentView) {
                    region.currentView.turnOnEditing();
                }
            });
        },
        turnOffEditing: function() {
            this.regionManager.forEach(function(region) {
                if (region.currentView) {
                    region.currentView.turnOffEditing();
                }
            });
        },
        getSchedulingConfiguration: function() {
            this.model.set({
                isScheduled: this.enableScheduling.currentView.model.getValue()[0],
                scheduleAmount: this.amountPicker.currentView.model.getValue()[0],
                scheduleUnit: this.unitPicker.currentView.model.getValue()[0],
                scheduleStart: this.startPicker.currentView.model.getValue()[0],
                scheduleEnd: this.endPicker.currentView.model.getValue()[0],
                deliveryIds: this.deliveryPicker.currentView.model.getValue()[0]
            });
            return this.model;

            // return {
            //     isScheduled: this.enableScheduling.currentView.model.getValue()[0],
            //     scheduleAmount: this.amountPicker.currentView.model.getValue()[0],
            //     scheduleUnit: this.unitPicker.currentView.model.getValue()[0],
            //     scheduleStart: this.startPicker.currentView.model.getValue()[0],
            //     scheduleEnd: this.endPicker.currentView.model.getValue()[0],
            //     scheduleSubscribers: scheduleSubscribers
            // };
        }
    });
});