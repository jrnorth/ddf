/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'underscore',
    'backbone',
    'js/Common',
    'backboneassociations'
], function(_, Backbone, Common) {

    return Backbone.Model.extend({
        defaults: {
            map: undefined,
            id: undefined,
            proxyEnabled: false,
            type: undefined,
            parameters: undefined,
            url: undefined,
            alpha: 0.8,
            customLayer: true
        },
        initialize: function () {
            if (!this.id){
                this.set('id', Common.generateUUID());
            }
        },
        validate: function(attrs, options) {
            if (!attrs.type || !attrs.url) {
                return "Unable to validate this WMS/WMT layer";
            }
            if (!map) {
                return "Cannot add layer to non-existent map";
            }
            if (!customLayer) {
                return "Custom layer must be true";
            }
        },
        toJSON: function() {
            return _.omit(_.clone(this.attributes), 'map');
        }
    });
})