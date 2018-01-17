"use strict";

// Similar to the IOU creation modal - see createIOUModal.js for comments.
angular.module('demoAppModule').controller('SelfIssueCashModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL) {
    const selfIssueCashModal = this;

    selfIssueCashModal.form = {};
    selfIssueCashModal.formError = false;

    selfIssueCashModal.issue = () => {
        if (invalidFormInput()) {
            selfIssueCashModal.formError = true;
        } else {
            selfIssueCashModal.formError = false;

            const amount = selfIssueCashModal.form.amount;
            const currency = selfIssueCashModal.form.currency;

            $uibModalInstance.close();

            const selfIssueCashEndpoint =
                apiBaseURL +
                `self-issue-cash?amount=${amount}&currency=${currency}`;

            $http.get(selfIssueCashEndpoint).then(
                (result) => {console.log(result.toString()); selfIssueCashModal.displayMessage(result); },
                (result) => {console.log(result.toString()); selfIssueCashModal.displayMessage(result); }
            );
        }
    };

    selfIssueCashModal.displayMessage = (message) => {
        const selfIssueCashMsgModal = $uibModal.open({
            templateUrl: 'selfIssueCashMsgModal.html',
            controller: 'selfIssueCashMsgModalCtrl',
            controllerAs: 'selfIssueCashMsgModal',
            resolve: {
                message: () => message
            }
        });

        selfIssueCashMsgModal.result.then(() => {}, () => {});
    };

    selfIssueCashModal.cancel = () => $uibModalInstance.dismiss();

    function invalidFormInput() {
        return isNaN(selfIssueCashModal.form.amount) || (selfIssueCashModal.form.currency.length != 3);
    }
});

angular.module('demoAppModule').controller('selfIssueCashMsgModalCtrl', function($uibModalInstance, message) {
    const selfIssueCashMsgModal = this;
    selfIssueCashMsgModal.message = message.data;
});