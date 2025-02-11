/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationClassIds
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithArguments
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForReceiverParameter private constructor(
    private val firCallableSymbol: FirCallableSymbol<*>,
    private val receiverParameter: FirAnnotationContainer,
    private val useSiteSession: FirSession,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {

    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            receiverParameter.resolvedAnnotationsWithArguments(firCallableSymbol).map { annotation ->
                annotation.toKtAnnotationApplication(useSiteSession)
            }
        }

    override fun hasAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        classId in receiverParameter.resolvedAnnotationClassIds(firCallableSymbol)
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        receiverParameter.resolvedAnnotationsWithArguments(firCallableSymbol).mapNotNull { annotation ->
            if (annotation.fullyExpandedClassId(useSiteSession) != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication(useSiteSession)
        }
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            receiverParameter.resolvedAnnotationClassIds(firCallableSymbol)
        }

    companion object {
        fun create(
            firCallableSymbol: FirCallableSymbol<*>,
            useSiteSession: FirSession,
            token: KtLifetimeToken,
        ): KtAnnotationsList {
            val receiverParameter = firCallableSymbol.receiverParameter
            return if (receiverParameter?.annotations?.isEmpty() != false) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForReceiverParameter(firCallableSymbol, receiverParameter, useSiteSession, token)
            }
        }
    }
}
