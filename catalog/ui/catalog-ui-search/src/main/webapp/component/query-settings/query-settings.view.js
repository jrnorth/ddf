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
    './query-settings.hbs',
    'js/CustomElements',
    'js/store',
    'js/model/QuerySchedule',
    'component/dropdown/dropdown',
    'component/dropdown/query-src/dropdown.query-src.view',
    'component/property/property.view',
    'component/property/property',
    'component/singletons/user-instance',
    'component/sort-item/sort-item.view',
    'component/query-schedule/query-schedule.view',
    'js/Common'
], function (Marionette, Backbone, _, $, template, CustomElements, store, QueryScheduleModel, DropdownModel,
            QuerySrcView, PropertyView, Property, user, SortItemView, ScheduleQueryView, Common) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('query-settings'),
        modelEvents: {
        },
        events: {
            'click .editor-edit': 'turnOnEditing',
            'click .editor-cancel': 'cancel',
            'click .editor-save': 'save',
            'click .editor-saveRun': 'run'
        },
        regions: {
            settingsSortField: '.settings-sorting-field',
            settingsSrc: '.settings-src',
            settingsSchedule: '.settings-scheduling'
        },
        ui: {
        },
        focus: function(){
        },
        initialize: function(){
            this.model = this.model._cloneOf ? store.getQueryById(this.model._cloneOf) : this.model;
            this.listenTo(this.model, 'change:sortField change:sortOrder change:src change:federation', Common.safeCallback(this.onBeforeShow));
        },
        onBeforeShow: function(){
            this.setupSortFieldDropdown();
            this.setupSrcDropdown();
            this.setupScheduling();
            this.turnOnEditing();
        },
        setupSortFieldDropdown: function() {
            this.settingsSortField.show(new SortItemView({
                model: new Backbone.Model({
                    attribute: this.model.get('sortField'),
                    direction: this.model.get('sortOrder')
                }),
                showBestTextOption: true
                
            }));
            this.settingsSortField.currentView.turnOffEditing();
            this.settingsSortField.currentView.turnOnLimitedWidth();
        },
        setupSrcDropdown: function(){
            var sources = this.model.get('src');
            this._srcDropdownModel = new DropdownModel({
                value: sources ? sources : [],
                federation: this.model.get('federation')
            });
            this.settingsSrc.show(new QuerySrcView({
                model: this._srcDropdownModel
            }));
            this.settingsSrc.currentView.turnOffEditing();
        },
        setupScheduling: function() {
            let username = user.get('user').get('userid');
            let scheduleModel = this.model.get('schedules').get(username);
            if (scheduleModel === undefined) {
                scheduleModel = new QueryScheduleModel({ userId: username });
            }
            this.settingsSchedule.show(new ScheduleQueryView({
                model: scheduleModel
            }));
            // this.settingsSchedule.show(new ScheduleQueryView({
            //     model: new Backbone.Model({
            //         isScheduled: this.model.get('schedule').get('isScheduled'),
            //         scheduleAmount: this.model.get('schedule').get('scheduleAmount'),
            //         scheduleUnit: this.model.get('schedule').get('scheduleUnit'),
            //         scheduleStart: this.model.get('schedule').get('scheduleStart'),
            //         scheduleEnd: this.model.get('schedule').get('scheduleEnd'),
            //         deliveryOptions: this.model.get('schedule').get('deliveryOptions')
            //     })
            // }));
            this.settingsSchedule.currentView.turnOffEditing();
        },
        turnOnEditing: function(){
           this.$el.addClass('is-editing');
            this.regionManager.forEach(function(region){
                if (region.currentView && region.currentView.turnOnEditing){
                    region.currentView.turnOnEditing();
                }
            });
            this.focus();
        },
        cancel: function(){
            this.$el.removeClass('is-editing');
            this.onBeforeShow();
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        toJSON: function() {
            var federation = this._srcDropdownModel.get('federation');
            var src;
            if (federation === 'selected') {
                src = this._srcDropdownModel.get('value');
                if (src === undefined || src.length === 0) {
                    federation = 'local';
                }
            }
            var sortField = this.settingsSortField.currentView.getSortField();
            var sortOrder = this.settingsSortField.currentView.getSortOrder();
            var scheduleModel = this.settingsSchedule.currentView.getSchedulingConfiguration();
            this.model.get('schedules').add(scheduleModel, {merge: true});
            return {
                src: src,
                federation: federation,
                sortField: sortField,
                sortOrder: sortOrder,
                schedules: this.model.get('schedules')
            };
        },
        saveToModel: function(){
            this.model.set(this.toJSON());
        },
        save: function(){
            this.saveToModel();
            this.cancel();
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        run: function(){
            this.saveToModel();
            this.cancel();
            this.model.startSearch();
            store.setCurrentQuery(this.model);
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        }
    });
});
