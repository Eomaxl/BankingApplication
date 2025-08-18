package com.eomaxl.bankapplication.service;

/**
 * Service Factory interface for creating service instances
 * Provides a centralized way to obtain service implementations
 * Useful for testing, dependency injection, and service composition
 */
public interface IServiceFactory {


    /**
     * Gets the person service implementation
     * @return Person service instance
     */
    IPersonService getPersonService();

    /**
     * Gets the bank service implementation
     * @return Bank service instance
     */
    IBankService getBankService();

    /**
     * Gets the account holder service implementation
     * @return Account holder service instance
     */
    IAccountHolderService getAccountHolderService();

    /**
     * Gets the account service implementation
     * @return Account service instance
     */
    IAccountService getAccountService();

    /**
     * Gets the transaction service implementation
     * @return Transaction service instance
     */
    ITransactionService getTransactionService();

    /**
     * Gets the banking facade service implementation
     * @return Banking facade service instance
     */
    IBankingFacadeService getBankingFacadeService();
}
