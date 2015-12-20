angular.module('app').factory('FeedbackService', function ($http, $q) {

  return {

    getFeedback: function (id, token){
      var deferred = $q.defer();

      $http.get('./wf/message/getMessage_Feedback?sID_Order='+id+'&sToken='+token+'').success(function (data, status) {
        deferred.resolve(data);
      }).error(function (data, status) {
        deferred.reject(true);
      });

      return deferred.promise;
    }
  }
});
