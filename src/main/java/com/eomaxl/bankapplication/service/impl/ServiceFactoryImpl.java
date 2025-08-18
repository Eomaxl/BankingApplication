package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Default implementation of the service factory
 * Provides access to all service implementations through a single factory
 */
@Component
@RequiredArgsConstructor
public class ServiceFactoryImpl implements IServiceFactory {

    private final IPersonService personService;
    private final IBankService bankService;
    private final IAccountHolderService accountHolderService;
    private final IAccountService accountService;
    private final ITransactionService transactionService;
    private final IBankingFacadeService bankingFacadeService;

    @Override
    public IPersonService getPersonService() {
        return personService;
    }

    @Override
    public IBankService getBankService() {
        return bankService;
    }

    @Override
    public IAccountHolderService getAccountHolderService() {
        return accountHolderService;
    }

    @Override
    public IAccountService getAccountService() {
        return accountService;
    }

    @Override
    public ITransactionService getTransactionService() {
        return transactionService;
    }

    @Override
    public IBankingFacadeService getBankingFacadeService() {
        return bankingFacadeService;
    }
}
