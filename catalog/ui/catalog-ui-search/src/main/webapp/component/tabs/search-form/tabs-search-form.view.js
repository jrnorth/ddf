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
 var _ = require('underscore');
 var $ = require('jquery');
 var TabsView = require('../tabs.view');
 var SearchFormModel = require('./tabs-search-form');
 var store = require('js/store');

 module.exports = TabsView.extend({
     selectionInterface: store,
     setDefaultModel: function(){
         this.model = new SearchFormModel();
     },
     initialize: function(options){
         this.setDefaultModel();
         this.options = options;
         TabsView.prototype.initialize.call(this);
     },
     determineContent: function() {
        var activeTab = this.model.getActiveView();
        this.tabsContent.show(new activeTab({
            model: this.options.model
        }));
     }
});