
var require = {
    baseUrl: 'jsc',
    paths: {
        'arbor': '../libs/cytoscape.js/lib/arbor',
        'async' : '../libs/requirejs-plugins/src/async',
        'atmosphere': '../libs/atmosphere-jquery/jquery.atmosphere',
        'bootstrap': '../libs/bootstrap/docs/assets/js/bootstrap',
        'bootstrap-datepicker': '../libs/bootstrap-datepicker/js/bootstrap-datepicker',
        'chrono': '../libs/chrono/chrono.min',
        'colorjs': '../libs/color-js/color',
        'cytoscape': '../libs/cytoscape.js/build/cytoscape',
        'd3': '../libs/d3/d3',
        'easing': '../libs/jquery.easing/js/jquery.easing',
        'ejs':  '../libs/ejs/ejs',
        'es5shim': '../libs/es5-shim/es5-shim',
        'es5sham': '../libs/es5-shim/es5-sham',
        'flight': '../libs/flight',
        'goog': '../libs/requirejs-plugins/src/goog',
        'intercom': '../libs/intercom/intercom',
        'jquery': '../libs/jquery/jquery',
        'jqueryui': '../libs/jquery-ui/ui/minified/jquery-ui.min',
        'openlayers': '../libs/openlayers/OpenLayers.debug',
        'pathfinding': '../libs/PathFinding.js/lib/pathfinding-browser',
        'propertyParser' : '../libs/requirejs-plugins/src/propertyParser',
        'rangy': '../libs/rangy-1.3/rangy-core',
        'rangy-text': '../libs/rangy-1.3/rangy-textrange',
        'scrollStop': '../libs/jquery-scrollstop/jquery.scrollstop',
        'sf': '../libs/sf/sf',
        'text': '../libs/requirejs-text/text',
        'three': '../libs/threejs/build/three',
        'tpl': '../libs/requirejs-ejs/rejs',
        'underscore': '../libs/underscore/underscore',
        'videojs': '../libs/video.js/video',
    },
    shim: {
        'atmosphere': { init: function() { return $.atmosphere; }, deps:['jquery'] },
        'bootstrap': { exports:'window', deps:['jquery', 'jqueryui'] },
        'bootstrap-datepicker': { exports:'window', deps:['bootstrap'] },
        'chrono': { exports: 'chrono' },
        'colorjs': { init: function() { return this.net.brehaut.Color; } },
        'cytoscape': { exports: 'cytoscape', deps:['arbor', 'easing'] },
        'd3': { exports: 'd3' },
        'easing': { init:function() { return $.easing; }, deps:['jquery', 'jqueryui'] },
        'ejs': { exports: 'ejs' },
        'intercom': { exports:'Intercom' },
        'jquery': { exports:'jQuery' },
        'jqueryui': { init: function() { return $.ui; }, deps:['jquery'] },
        'openlayers': { exports: 'OpenLayers', deps:['goog!maps,3,other_params:sensor=false'] },
        'pathfinding': { exports: 'PF' },
        'rangy-text': { deps:['rangy']},
        'scrollStop': { exports: 'jQuery', deps:['jquery'] },
        'three': { exports: 'THREE' },
        'underscore': { exports: '_' },
        'videojs': { exports: 'videojs' }
    }
};


if ('define' in window) {
    define([], function() {
        return require;
    });
}
