/**
 * Created by weichunhe on 2015/12/29.
 */

(function (_t) {
    // APPROOT 在home.html中定义，表示工程的根目录

    var min_suffix = _t.UGLIFY ? '-min' : '', // 是否使用压缩文件
        ROOT = APPROOT + 'service/asset/', BASE = ROOT + 'js/', LIB = ROOT + 'lib/', BOWER_ROOT = ROOT
            + 'bower_components/';
    var config = {
        baseUrl: BASE,
        paths: {
            jquery: BOWER_ROOT + 'AdminLTE/plugins/jQuery/jquery-2.2.3.min',
            angular: BOWER_ROOT + 'angular/angular.min',
            route: BOWER_ROOT + 'angular-ui-router/release/angular-ui-router.min',
            lodash: BOWER_ROOT + 'lodash/lodash.min',
            bootstrap: BOWER_ROOT + 'AdminLTE/bootstrap/js/bootstrap.min',
            datepicker: BOWER_ROOT + 'eonasdan-bootstrap-datetimepicker/build/js/bootstrap-datetimepicker.min',
            moment: BOWER_ROOT + 'moment/min/moment.min',
            extend: LIB + 'angular-extend',
            angular_ace_builds: BOWER_ROOT + 'ace-builds/src-min-noconflict/ace',
            angular_ui_ace: BOWER_ROOT + 'angular-ui-ace/ui-ace.min',
            adminlte: BOWER_ROOT + 'AdminLTE/dist/js/app.min',
            jsonRpc: LIB + 'jquery.jsonrpc',
            base: LIB + 'base'

        },
        shim: {
            angular: {
                deps: ['jquery', 'angular_ace_builds']
            },
            angular_ui_ace: {
                deps: ['angular']
            },
            route: {
                deps: ['angular']
            },
            jsonRpc: {
                deps: ['jquery']
            },
            bootstrap: {
                deps: ['jquery']
            },
            datepicker: {
                deps: ['jquery', 'bootstrap']
            },
            adminlte: {
                deps: ['bootstrap']
            },
            extend: {
                deps: ['angular']
            },
            base: {
                deps: ['route', 'lodash', 'datepicker', 'extend', 'adminlte', 'angular_ui_ace', 'jsonRpc']
            }
        }
    };

    requirejs.config(config);
})(this);
