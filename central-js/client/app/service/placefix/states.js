// moded from service\region\serviceRegion.states.js

angular.module('app').config(function($stateProvider) {
  $stateProvider
  // .state('index.service.general.placefix', { // country
  //   url: '/placefix',
  //   views: {
  //     'main@': {
  //       templateUrl: 'app/service/index.html',
  //       controller: 'PlaceFixController'
  //         // controller: 'ServiceC ountryController'
  //     }
  //   }
  // })
  // .state('index.service.general.placefix.error', { // country
  //   url: '/absent',
  //   views: {
  //     'content@index.service.general.placefix': {
  //       templateUrl: 'app/service/placefix/absent.html',
  //       controller: 'PlaceFixController'
  //         // controller: 'ServiceC ountryAbsentController'
  //     }
  //   }
  // })
    .state('index.service.general.placefix', { // region
      url: '/placefix',
      // FIXME PREVENT Coloring
      // resolve: {
      //   regions: function($stateParams, PlacesService, service) {
      //     return PlacesService.getRegions().then(function(response) {
      //       var regions = response.data;
      //       var aServiceData = service.aServiceData;

      //       angular.forEach(regions, function(region) {
      //         var color = 'red';
      //         angular.forEach(aServiceData, function(oServiceData) {
      //           if (oServiceData.hasOwnProperty('nID_Region') == false) {
      //             return;
      //           }
      //           var oRegion = oServiceData.nID_Region;
      //           if (oRegion.nID == region.nID) {
      //             color = 'green';
      //           }
      //         });
      //         region.color = color;
      //       });
      //       return regions;
      //     });
      //   }
      // },
      views: {
        'main@': {
          templateUrl: 'app/service/placefix/index.html',
          // controller: 'ServiceRegionController'
          controller: 'PlaceFixController'
        },
        'content@index.service.general.placefix': {
          templateUrl: 'app/service/placefix/content.html'
        }
      }
    })
    .state('index.service.general.placefix.error', { // region
      url: '/absent',
      views: {
        'content@index.service.general.placefix': {
          templateUrl: 'app/service/placefix/absent.html',
          controller: 'PlaceFixController'
        }
      }
    });
});