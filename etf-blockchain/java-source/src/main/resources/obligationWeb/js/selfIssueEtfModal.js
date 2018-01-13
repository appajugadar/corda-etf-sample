"use strict";

// Similar to the IOU creation modal - see createIOUModal.js for comments.
angular.module('demoAppModule').controller('SelfIssueEtfModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL) {
    const selfIssueEtfModal = this;

    selfIssueEtfModal.form = {};
    selfIssueEtfModal.formError = false;

    selfIssueEtfModal.issue = () => {
        if (invalidFormInput()) {
            selfIssueEtfModal.formError = true;
        } else {
            selfIssueEtfModal.formError = false;

            const quantity = selfIssueEtfModal.form.quantity;
            const etfName = selfIssueEtfModal.form.etfName;

            $uibModalInstance.close();

            const selfIssueEtfEndpoint =
                apiBaseURL +
                `self-issue-etf?quantity=${quantity}&etfName=${etfName}`;

            $http.get(selfIssueEtfEndpoint).then(
                (result) => {console.log(result.toString()); selfIssueEtfModal.displayMessage(result); },
                (result) => {console.log(result.toString()); selfIssueEtfModal.displayMessage(result); }
            );
        }
    };

    selfIssueEtfModal.displayMessage = (message) => {
        const selfIssueEtfMsgModal = $uibModal.open({
            templateUrl: 'selfIssueEtfMsgModal.html',
            controller: 'selfIssueEtfMsgModalCtrl',
            controllerAs: 'selfIssueEtfMsgModal',
            resolve: {
                message: () => message
            }
        });

        selfIssueEtfMsgModal.result.then(() => {}, () => {});
    };

    selfIssueEtfModal.cancel = () => $uibModalInstance.dismiss();

    function invalidFormInput() {
        return isNaN(selfIssueEtfModal.form.quantity) || (selfIssueEtfModal.form.etfName.length != 3);
    }
});

angular.module('demoAppModule').controller('selfIssueEtfMsgModalCtrl', function($uibModalInstance, message) {
    const selfIssueEtfMsgModal = this;
    selfIssueEtfMsgModal.message = message.data;
});