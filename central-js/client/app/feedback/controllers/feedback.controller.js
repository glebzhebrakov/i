angular.module('feedback').controller('FeedbackController', function($state, FeedbackService) {

  FeedbackService.getFeedback(1,12345).then(function(data){
      console.log("feedback");
  });
});
