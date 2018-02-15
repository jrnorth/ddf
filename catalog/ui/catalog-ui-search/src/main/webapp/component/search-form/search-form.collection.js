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
 var _ = require('underscore');
 var $ = require('jquery');
 var Backbone = require('backbone');
 var SearchForm = require('./search-form');

 module.exports = Backbone.Collection.extend({
   model: SearchForm,
   initialize: function(models, options) {
    this.getCustomQueryForms();
   },
   getCustomQueryForms: function() {
        var that = this;
        $.ajax({
            type: 'GET',
            url: '/search/catalog/internal/forms/query',
            contentType: 'application/json',
            success: function (data) {
                that.saveExport(data)
            },
            failure: function () {
                this.$el.trigger("doneLoading");

            }
        });
        this.add(new SearchForm({type: 'basic'}));
        this.add(new SearchForm({type: 'text'}));
   },
   saveExport: function(data) {
        var that = this;
        $.each(data, function(index, value) {
            that.add(new SearchForm({id: value.id, name: value.title, type: 'custom', formTemplate: value}));
        });
        this.trigger("doneLoading");
   }
 });