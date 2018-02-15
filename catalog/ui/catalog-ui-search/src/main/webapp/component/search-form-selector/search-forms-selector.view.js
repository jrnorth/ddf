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
var template = require('./search-forms-selector.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var $ = require('jquery');
var SearchFormsView = require('component/tabs/search-form/tabs-search-form.view');

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('search-form-system'),
    template: template,
    regions: {
        tabsContent: {
            selector: '.search-form-system-content'
        },
        tabsTitle: {
            selector: '.search-form-system-title'
        }
    },
    onRender: function () {
        this.tabsContent.show(new SearchFormsView({
            model: this.model
        }));
    }
});