"use strict";

angular.module('demoAppModule').controller('CreateETFModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const createETFModal = this;

    createETFModal.peers = peers;
    createETFModal.form = {};
    createETFModal.formError = false;

    /** Validate and create an ETF. */
    createETFModal.create = () => {
        if (invalidFormInput()) {
            createETFModal.formError = true;
        } else {
            createETFModal.formError = false;

            const buysell = createETFModal.form.buysell;
            const counterparty = createETFModal.form.counterparty;
            const etfName = createETFModal.form.etfName;
            const currency = createETFModal.form.currency;
            const quantity = createETFModal.form.quantity;
            const amount = quantity*10;
            $uibModalInstance.close();

            // We define the ETF creation endpoint.
            const issueETFEndpoint =
                apiBaseURL +
                `issue-etf-buy-sell?buysell=${buysell}&counterparty=${counterparty}&etfName=${etfName}&currency=${currency}&quantity=${quantity}&amount=${amount}`;

            // We hit the endpoint to create the ETF and handle success/failure responses.
            $http.get(issueETFEndpoint).then(
                (result) => createETFModal.displayMessage(result),
                (result) => createETFModal.displayMessage(result)
            );
        }
    };

    /** Displays the success/failure response from attempting to create an ETF. */
    createETFModal.displayMessage = (message) => {
        const createETFMsgModal = $uibModal.open({
            templateUrl: 'createETFMsgModal.html',
            controller: 'createETFMsgModalCtrl',
            controllerAs: 'createETFMsgModal',
            resolve: {
                message: () => message
            }
        });

        // No behaviour on close / dismiss.
        createETFMsgModal.result.then(() => {}, () => {});
    };

    /** Closes the ETF creation modal. */
    createETFModal.cancel = () => $uibModalInstance.dismiss();

    // Validates the ETF.
    function invalidFormInput() {
        return isNaN(createETFModal.form.quantity) || (createETFModal.form.counterparty === undefined);
    }
});

// Controller for the success/fail modal.
angular.module('demoAppModule').controller('createETFMsgModalCtrl', function($uibModalInstance, message) {
    const createETFMsgModal = this;
    createETFMsgModal.message = message.data;
});